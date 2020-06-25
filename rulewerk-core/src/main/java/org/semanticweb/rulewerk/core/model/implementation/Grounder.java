package org.semanticweb.rulewerk.core.model.implementation;

/*-
 * #%L
 * Rulewerk Core Components
 * %%
 * Copyright (C) 2018 - 2020 Rulewerk Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import karmaresearch.vlog.NotStartedException;
import karmaresearch.vlog.Term.TermType;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.rulewerk.core.model.api.*;
import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.longs.AbstractLong2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/**
 * Class for grounding asp rules and facts. The grounder uses a file writer and a reasoner that has the (asp) facts
 * materialized. Moreover, the grounder utilizes the knowledge about approximated predicates to omit facts that are
 * certainly true. The grounder creates, on-the-fly, an index containing the integers of the grounded literals.
 */
public class Grounder implements AspRuleVisitor<Boolean> {

	final private KnowledgeBase knowledgeBase;
	final private Set<Predicate> approximatedPredicates;
	final private List<Predicate> approximatedPredicatesList;
	final private Reasoner reasoner;
	final private BufferedWriter writer;
	final private boolean textFormat;
	final private int numberOfConstants;
	final private int numberOfRules;
	final private int numberOfPredicates;
	final private AbstractLong2IntMap aspifMap;
	private int aspifCounter;

	/**
	 * The constructor.
	 *
	 * @param reasoner the reasoner with the information for the grounding
	 * @param knowledgeBase the knowledge base for which the grounder should be used
	 * @param writer a file writer for writing the grounded rules
	 * @param approximatedPredicates set of approximated predicates
	 * @param textFormat whether to ground in text format or not
	 */
	public Grounder(Reasoner reasoner, KnowledgeBase knowledgeBase, BufferedWriter writer, Set<Predicate> approximatedPredicates, boolean textFormat) {
		this.knowledgeBase = knowledgeBase;
		this.reasoner = reasoner;
		this.writer = writer;
		this.approximatedPredicates = approximatedPredicates;
		this.approximatedPredicatesList = new LinkedList<>(approximatedPredicates);
		this.textFormat = textFormat;
		this.numberOfConstants = knowledgeBase.getConstants().size();
		this.numberOfRules = knowledgeBase.getAspRules().size();
		this.numberOfPredicates = approximatedPredicates.size();
		this.aspifMap = new Long2IntOpenHashMap();
		this.aspifCounter = 1;
	}

	/**
	 * Ground the knowledge base.
	 */
	public void groundKnowledgeBase() {
		if (this.textFormat) {
			this.knowledgeBase.getFacts().forEach(fact -> {
				try {
					writer.write(fact.getSyntacticRepresentation() + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			this.knowledgeBase.getAspRules().forEach(rule -> {
				rule.accept(this);
			});

			// TODO: Ground show statements
		} else {
			try {
				this.writer.write("asp 1 0 0");
				this.writer.newLine();

				this.knowledgeBase.getFacts().forEach(this::writeFactAspif);
				this.knowledgeBase.getAspRules().forEach(rule -> {
					rule.accept(this);
				});
				this.knowledgeBase.getShowStatements().forEach(this::groundShowStatement);

				this.writer.write("0");
				this.writer.newLine();
			} catch (IOException e) {
				System.out.println("An error occurred.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public Boolean visit(ChoiceRule rule) {
		try {
			this.groundRule(rule);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public Boolean visit(Constraint rule) {
		try {
			this.groundRule(rule, false);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public Boolean visit(DisjunctiveRule rule) {
		try {
			this.groundRule(rule, true);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	// ========== General parts for all grounding formats ==========

	/**
	 * Ground the given show statement.
	 *
	 * @param statement the show statement to ground
	 */
	public void groundShowStatement(ShowStatement statement) {
		PositiveLiteral literal = statement.getQueryLiteral();
		Predicate predicate = literal.getPredicate();
		long predicateId = getPredicateIndex(literal.getPredicate());

		try (final karmaresearch.vlog.QueryResultIterator answers = reasoner.answerQueryInNativeFormat(literal, true)) {
			// each query result represents a grounding
			while(answers.hasNext()) {
				long[] termIds = answers.next();
				try {
					writeShowStatementAspif(predicate, predicateId, termIds);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (NotStartedException e) {
					// Should not happen, we just did a query ...
				}
			}
		}
	}

	/**
	 * Grounds an asp rule (constraint or disjunctive rule) and writes it via the file writer
	 *
	 * @param disjunctiveRule whether the rule is a disjunctive rule
	 * @param rule the rule to ground
	 */
	public void groundRule(AspRule rule, boolean disjunctiveRule) throws IOException {
		// If the rule head has exactly one literal, whose predicate is not approximated, there is no reason to ground
		// the rule because VLog knows already which of the grounded literals are true.
		List<PositiveLiteral> headLiterals = rule.getHeadLiterals().getLiterals();
		if (!(headLiterals.size() == 1 && !approximatedPredicates.contains(headLiterals.get(0).getPredicate()))) {
			PositiveLiteral literal = rule.getHelperLiteral();
			Map<Variable, Long> answerMap = new HashMap<>();
			List<Variable> variables = literal.getUniversalVariables().collect(Collectors.toList());

			long startTime = System.nanoTime();
			int counter = 0;

			try (final karmaresearch.vlog.QueryResultIterator answers = reasoner.answerQueryInNativeFormat(literal, true)) {
				// each query result represents a grounding
				while(answers.hasNext()) {
					counter++;
					long[] terms = answers.next();
					for (int i = 0; i < terms.length; i++) {
						answerMap.put(variables.get(i), terms[i]);
					}

	//				if (this.textFormat) {
	//					groundedRule = rule.ground(approximatedPredicates, answerMap);
	//					try {
	//						writer.write(groundedRule);
	//					} catch (IOException e) {
	//						System.out.println("An error occurred.");
	//						e.printStackTrace();
	//					}
	//				} else {
					writeRuleInstanceAspif(rule, answerMap, disjunctiveRule);
	//				}
				}
			}

			long endTime = System.nanoTime();
			System.out.println(rule.getSyntacticRepresentation());
			System.out.println("Duration: " + ((endTime - startTime) / 1000000) + " ms");
			System.out.println("Instances: " + counter);
			if (counter > 0) {
				System.out.println("Duration per instance: " + ((endTime - startTime) / counter ) + " ns");
			}
			System.out.println();
		}
	}

	/**
	 * ground an asp choice rule and write via the file writer
	 *
	 * @param rule the rule to ground
	 */
	public void groundRule(ChoiceRule rule) throws IOException {
		long startTime = System.nanoTime();
		List<UniversalVariable> relevantGlobalVariables = rule.getRelevantGlobalVariables().collect(Collectors.toList());
		PositiveLiteral bodyLiteral = rule.getHelperLiteral("body", new ArrayList<>(relevantGlobalVariables), rule.getRuleIdx());

		Map<Variable, Long> answerMapHeadVariables = new HashMap<>();
		int counter = 0;
		try (final karmaresearch.vlog.QueryResultIterator answersBody = reasoner.answerQueryInNativeFormat(bodyLiteral, true)) {
			// each query result represents a grounding (= grounding of the global variables)

			while (answersBody.hasNext()) {
				counter++;
				long[] terms = answersBody.next();
				for (int i = 0; i < terms.length; i++) {
					answerMapHeadVariables.put(relevantGlobalVariables.get(i), terms[i]);
				}

				// helper integer for body (get and write)
				long[] termIds = getTermIds(bodyLiteral, answerMapHeadVariables);
				int bodyHelpInteger = getAndWriteBodyHelpInteger(rule, bodyLiteral, answerMapHeadVariables);

				Map<Integer, List<Integer>> choiceSetMap = new HashMap<>();
				int idx = 0;
				for (ChoiceElement choiceElement : rule.getChoiceElements()) {
					addChoiceElementAspifToMap(choiceSetMap, choiceElement, rule, answerMapHeadVariables, idx);
					idx++;
				}

				Set<Integer> chosenLiteralSet = new HashSet<>();

				// for each condition: add a choice statement
				for (Integer conditionInteger : choiceSetMap.keySet()) {
					List<Integer> literalIntegers = choiceSetMap.get(conditionInteger);
					List<Integer> choiceSetBodyIntegers = new ArrayList<>();

					if (bodyHelpInteger > 0) {
						choiceSetBodyIntegers.add(bodyHelpInteger);
					}
					if (conditionInteger > 0) {
						choiceSetBodyIntegers.add(conditionInteger);
					}

					// { choice element integers } :- body integer, condition integer
					writer.write("1 1 " // rule statement for a choice rule
						+ literalIntegers.size() + " " // count of selectable literals
						+ StringUtils.join(literalIntegers, " ") // the literals
						+ " 0 " + choiceSetBodyIntegers.size() + " " // normal body
						+ StringUtils.join(choiceSetBodyIntegers, " ")); // body and condition
					writer.newLine();

					if (rule.hasLowerBound() || rule.hasUpperBound()) {
						for (Integer literalInteger : literalIntegers) {
							if (conditionInteger > 0) {
								// literal counts integer :- literal integer, condition integer
								int countLiteralInteger = getAspifValue(numberOfPredicates - 1 + rule.getRuleIdx(), false, new long[]{literalInteger}, 3);

								writer.write("1 0 1 "  // rule statement for a disjunctive rule with a single head literal
									+ countLiteralInteger
									+ " 0 2 "
									+ literalInteger + " " + conditionInteger);
								writer.newLine();

								// collect element counts integer
								chosenLiteralSet.add(countLiteralInteger);
							} else {
								chosenLiteralSet.add(literalInteger);
							}
						}
					}
				}

				// if there are bounds, take care that they are satisfied
				if (rule.hasLowerBound()) {
					// introduce integer to check if enough elements has been chosen
					long lowerBoundInteger = getAspifValue(numberOfPredicates - 1 + rule.getRuleIdx(), false, termIds, 1);

					writer.write("1 0 1 " + lowerBoundInteger // rule statement for a disjunctive rule with a single head literal
						+ " 1 " + rule.getLowerBound() + " " + chosenLiteralSet.size() + " " // weighted body
						+ StringUtils.join(chosenLiteralSet, " 1 ") + " 1" // elements with weight 1
					);
					writer.newLine();

					if (bodyHelpInteger > 0) {
						writer.write("1 0 0 0 2 " + bodyHelpInteger + " -" + lowerBoundInteger);
					} else {
						writer.write("1 0 0 0 1 -" + lowerBoundInteger);
					}
					writer.newLine();
				}

				if (rule.hasUpperBound()) {
					// introduce integer to check if too many elements has been chosen
					long upperBoundInteger = getAspifValue(numberOfPredicates - 1 + rule.getRuleIdx(), false, termIds, 2);
					writer.write("1 0 1 " + upperBoundInteger // rule statement for a disjunctive rule with a single head literal
						+ " 1 " + (rule.getUpperBound() + 1) + " " + chosenLiteralSet.size() + " " // weighted body
						+ StringUtils.join(chosenLiteralSet, " 1 ") + " 1" // elements with weight 1
					);
					writer.newLine();

					if (bodyHelpInteger > 0) {
						writer.write("1 0 0 0 2 " + bodyHelpInteger + " " + upperBoundInteger);
					} else {
						writer.write("1 0 0 0 1 " + upperBoundInteger);
					}
					writer.newLine();
				}
//				if (this.textFormat) {
//					// ground choice with placeholder for choice elements
//					String groundedRule = rule.ground(approximatedPredicates, answerMap);
//
//					Object[] choiceElements = new String[rule.getChoiceElements().size()];
//					int idx = 0;
//					// each choice element contributes individual grounded elements
//					for (ChoiceElement choiceElement : rule.getChoiceElements()) {
//						choiceElements[idx] = groundChoiceElement(choiceElement, rule, answerMap, idx);
//						idx++;
//					}
//
//					try {
//						writer.write(String.format(groundedRule, choiceElements));
//					} catch (IOException e) {
//						System.out.println("An error occurred.");
//						e.printStackTrace();
//					}
//				} else {
//				}
			}
		}

		long endTime = System.nanoTime();
		System.out.println(rule.getSyntacticRepresentation());
		System.out.println("Duration: " + ((endTime - startTime) / 1000000) + " ms");
		System.out.println("Instances: " + counter);
		if (counter > 0) {
			System.out.println("Duration per instance: " + ((endTime - startTime) / counter ) + " ns");
		}
		System.out.println();
	}

	/**
	 * Returns the integer for the body integer. If there are several possibles ways to infer a grounded body instance,
	 * a help integers is introduced (and returned) and the corresponding rules to get the body help integer are written.
	 * It might be possible to get the body integer in several ways if the body contains variables that do not occur in
	 * the rule head.
	 *
	 * @param rule the rule for which the body is considered
	 * @param bodyLiteral the literal containing all variables that occur in both the rule body and head
	 * @param answerMapHeadVariables grounding for those variables
	 * @return an integer representing the rule body
	 * @throws IOException due to writing to file
	 */
	private int getAndWriteBodyHelpInteger(ChoiceRule rule, PositiveLiteral bodyLiteral, Map<Variable, Long> answerMapHeadVariables) throws IOException {
		int approximatedLiteralCount = rule.getBody().getRelevantLiteralCount(approximatedPredicates);
		if (approximatedLiteralCount == 0) {
			return 0;
		} else if (approximatedLiteralCount == 1 && rule.getBodyOnlyGlobalVariables().count() == 0) {
			Literal literal = rule.getBody().getLiterals().stream().filter(literal1 -> approximatedPredicates.contains(literal1.getPredicate())).findFirst().orElse(bodyLiteral);
			long[] termIds = getTermIds(literal, answerMapHeadVariables);
			return getAspifValue(getPredicateIndex(literal.getPredicate()), literal.isNegated(), termIds);
		} else {
			long[] termIds = getTermIds(bodyLiteral, answerMapHeadVariables);
			int bodyHelpInteger = getAspifValue(numberOfPredicates - 1 + rule.getRuleIdx(), false, termIds, 4);

			List<Term> partiallyGroundedVariables = rule.getGlobalVariables()
				.map(variable -> getTermFromPartialAnswer(variable, answerMapHeadVariables))
				.collect(Collectors.toList());
			Map<Variable, Long> answerMapBodyVariables = new HashMap<>(answerMapHeadVariables);
			PositiveLiteral bodyAllLiteral = rule.getHelperLiteral("bodyAll", partiallyGroundedVariables, rule.getRuleIdx());

			try (final karmaresearch.vlog.QueryResultIterator answersBodyAll = reasoner.answerQueryInNativeFormat(bodyAllLiteral, true)) {

				while (answersBodyAll.hasNext()) {
					// build the map that represents the completely (locally and globally) ground rule
					long[] allTermsId = answersBodyAll.next();
					for (int i = 0; i < partiallyGroundedVariables.size(); i++) {
						Term globalTerm = partiallyGroundedVariables.get(i);
						if (globalTerm.isVariable()) {
							answerMapBodyVariables.put((Variable) globalTerm, allTermsId[i]);
						}
					}

					writer.write("1 0 1 " + // rule statement for disjunctive rule with single head literal
						bodyHelpInteger // for the body help integer
					);
					writeNormalBodyAspif(rule.getBody(), answerMapBodyVariables);
				}
			}

			return bodyHelpInteger;
		}
	}

	// ========== Text-based grounding part ==========

	/**
	 * Returns the grounding of a single choice element
	 *
	 * @param choiceElement the element to ground
	 * @param rule the rule the element belongs to
	 * @param globalMap a map for the global variables
	 * @param idx the index of the choice element
	 * @return a string that represents all instances of the choice element
	 */
	public String groundChoiceElement(ChoiceElement choiceElement, ChoiceRule rule, Map<Variable, Term> globalMap, int idx) {
		List<Term> terms = Stream.concat(rule.getBody().getUniversalVariables(), choiceElement.getContext().getUniversalVariables())
								 .distinct()
								 .map(variable -> globalMap.getOrDefault(variable, variable))
								 .collect(Collectors.toList());

		Map<Variable, Term> map = new HashMap<>(globalMap);
		PositiveLiteral literal = rule.getHelperLiteral(terms, rule.getRuleIdx(), idx);
		final QueryResultIterator answers = reasoner.answerQuery(literal, true);

		StringBuilder builder = new StringBuilder();
		boolean first = true;
		while (answers.hasNext()) {
			if (first) {
				first = false;
			} else {
				builder.append("; ");
			}

			List<Term> localTerms = answers.next().getTerms();
			for (int i = 0; i < terms.size(); i++) {
				Term globalTerm = terms.get(i);
				if (globalTerm.isVariable()) {
					map.put((Variable) globalTerm, localTerms.get(i));
				}
			}

			builder.append(choiceElement.ground(approximatedPredicates, map));
		}

		return builder.toString();
	}

	// ========== Aspif-specific part ==========

	/**
	 * Write the instance of the rule as it is specified by the answer map in aspif.
	 * @param rule the rule
	 * @param answerMap the map representing the instance
	 * @param disjunctiveRule whether the rule is a disjunctive rule
	 * @throws IOException exception from writing to file
	 */
	private void writeRuleInstanceAspif(AspRule rule, Map<Variable, Long> answerMap, boolean disjunctiveRule) throws IOException {
		writer.write("1 0"); // rule statement for a disjunctive rule
		if (disjunctiveRule) {
			writer.write(" " + rule.getHeadLiterals().getLiterals().size()); // #headLiterals
			for (Literal literal : rule.getHeadLiterals().getLiterals()) {
				long predicateId = getPredicateIndex(literal.getPredicate());
				long[] termIds = getTermIds(literal, answerMap);
				writer.write(" " + getAspifValue(predicateId, literal.isNegated(), termIds));
			}
		} else {
			writer.write(" 0"); // #headLiteral = 0
		}

		writeNormalBodyAspif(rule.getBody(), answerMap);
	}

	/**
	 * Add the integer for the literal of a choice element to the integer set (a.k.a choice set) which contains all the
	 * integers with the same condition.
	 *
	 * @param choiceSetMap a map where the literal integer sets are stored corresponding to their condition
	 * @param choiceElement the choice element to handle
	 * @param rule the rule the choice element belongs to
	 * @param answerMapHeadVariables the answer map for grounding the head variables
	 * @param idx the index of the choice element
	 */
	private void addChoiceElementAspifToMap(Map<Integer, List<Integer>> choiceSetMap, ChoiceElement choiceElement, ChoiceRule rule, Map<Variable, Long> answerMapHeadVariables, int idx) throws IOException {
		// Get all the variables and terms used by the body and the condition of the choice element
		// For the variables: Replace them with the constant if they are part of the grounding of the body

		List<Term> terms = Stream.concat(
			rule.getBody().getUniversalVariables(),
			choiceElement.getContext().getUniversalVariables()
		).distinct().map(
			variable -> getTermFromPartialAnswer(variable, answerMapHeadVariables)
		).collect(Collectors.toList());

		Map<Variable, Long> answerMapChoiceElement = new HashMap<>(answerMapHeadVariables);
		PositiveLiteral literal = rule.getHelperLiteral(terms, rule.getRuleIdx(), idx);
		try (final karmaresearch.vlog.QueryResultIterator answers = reasoner.answerQueryInNativeFormat(literal, true)) {
			long predicateId = getPredicateIndex(choiceElement.getLiteral().getPredicate());

			while (answers.hasNext()) {
				// build the map that represents the completely (locally and globally) ground rule
				long[] localTerms = answers.next();
				for (int i = 0; i < terms.size(); i++) {
					Term term = terms.get(i);
					if (term instanceof Variable) {
						answerMapChoiceElement.put((Variable) term, localTerms[i]);
					}
				}

				// get the integer for the literal of the choice element
				long[] termIds = getTermIds(choiceElement.getLiteral(), answerMapChoiceElement);
				int literalInteger = getAspifValue(predicateId, false, termIds);

				// get the integers for the literals of the condition of the choice element
				// ignore not approximated literals
				List<Integer> conditionIntegerList = choiceElement.getContext().getLiterals().stream().filter(
					literal1 -> approximatedPredicates.contains(literal1.getPredicate())
				).map(
					literal1 -> getAspifValue(getPredicateIndex(literal1.getPredicate()), literal1.isNegated(), getTermIds(literal1, answerMapChoiceElement))
				).collect(Collectors.toList());
				// an empty list has to be handled differently
				int conditionInteger = conditionIntegerList.isEmpty()
					? 0
					: getAspifValue(numberOfPredicates - 1 + rule.getRuleIdx(), false, getTermIds(conditionIntegerList), 0);

				if (choiceSetMap.containsKey(conditionInteger)) {
					choiceSetMap.get(conditionInteger).add(literalInteger);
				} else {
					if (conditionInteger != 0) {
						writer.write("1 0 1 " + conditionInteger); // rule statement for a disjunctive rule with a single literal
						writeNormalBodyAspif(choiceElement.getContext(), answerMapChoiceElement);
					}
					choiceSetMap.put(conditionInteger, new ArrayList<>(Collections.singletonList(literalInteger)));
				}
			}

		}
	}

	/**
	 * Write the the body instance given by the answer map in aspif.
	 *
	 * @param body the conjunction of literals
	 * @param answerMap the map representing the body instance
	 * @throws IOException possible exception due to writing to file
	 */
	private void writeNormalBodyAspif(Conjunction<Literal> body, Map<Variable, Long> answerMap) throws IOException {
		writer.write(" 0"); // normal body
		writer.write(" " + body.getRelevantLiteralCount(approximatedPredicates));
		writeConjunctionAspif(body, answerMap);
		writer.write("\n");
	}

	/**
	 * Write the integers representing the literals of the conjunction for the instance given by the answerMap.
	 * Ignore literals that are not approximated, as they are always true.
	 * @param conjunction the conjunction to get the aspif integers for
	 * @param answerMap a map representing the instance of the conjunction
	 * @throws IOException an exception due to writing to a file
	 */
	private void writeConjunctionAspif(Conjunction<Literal> conjunction, Map<Variable, Long> answerMap) throws IOException {
		for (Literal literal : conjunction.getLiterals()) {
			if (approximatedPredicates.contains(literal.getPredicate())) {
				long predicateId = getPredicateIndex(literal.getPredicate());
				long[] termIds = getTermIds(literal, answerMap);
				writer.write(" " +  getAspifValue(predicateId, literal.isNegated(), termIds));
			}
		}
	}

	/**
	 * Write the given fact in aspif
	 *
	 * @param fact the fact to write
	 */
	public void writeFactAspif(Fact fact) {
		if (approximatedPredicates.contains(fact.getPredicate())) {
			long predicateId = getPredicateIndex(fact.getPredicate());
			long[] termIds = getTermIds(fact);
			String aspifFact = "1 0 1 " + getAspifValue(predicateId, false, termIds) + " 0 0\n";
			try {
				writer.write(aspifFact);
			} catch (IOException e) {
				System.out.println("An error occurred.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Write the instance of a show statement.
	 *
	 * @param predicate the predicate
	 * @param predicateId the predicate id
	 * @param termIds the term ids
	 * @throws IOException an IOException
	 * @throws NotStartedException a VLog exception
	 */
	public void writeShowStatementAspif(Predicate predicate, long predicateId, long[] termIds) throws IOException, NotStartedException {
		writer.write("4 "); // show statement

		StringBuilder symbolicRepresentation = new StringBuilder();
		symbolicRepresentation.append(predicate.getName());
		boolean firstConstant = true;
		for (long termId : termIds) {
			if (firstConstant) {
				symbolicRepresentation.append("(");
				firstConstant = false;
			} else {
				symbolicRepresentation.append(",");
			}

			symbolicRepresentation.append(reasoner.getConstant(termId));
		}
		symbolicRepresentation.append(")");

		writer.write(symbolicRepresentation.length() + " ");
		writer.write(symbolicRepresentation.toString());

		if (approximatedPredicates.contains(predicate)) {
			writer.write(" 1 " + getAspifValue(predicateId, false, termIds));
		} else {
			writer.write(" 0");
		}

		writer.newLine();
	}

	/**
	 * Get the integer that represents a ground literal.
	 *
	 * @param predicateId the predicate id
	 * @param negated whether the literal is negated
	 * @param termIds array of term ids
	 * @param context context in which the literal is used
	 * @return the aspif integer
	 */
	private int getAspifValue(long predicateId, boolean negated, long[] termIds, long... context) {
		long aspifLongIdentifier = 0;
		long base = 1 + Math.max(numberOfConstants, numberOfRules);
		for (long id : context) {
			aspifLongIdentifier = aspifLongIdentifier * base + id + 1;
		}
		for (long id : termIds) {
			aspifLongIdentifier = aspifLongIdentifier * base + id + 1;
		}
		aspifLongIdentifier = aspifLongIdentifier * (numberOfPredicates + numberOfRules) + predicateId;
//		StringBuilder aspifLongIdentifier = new StringBuilder();
//		aspifLongIdentifier.append(predicateId);
//		for (long id : context) {
//			aspifLongIdentifier.append(":").append(id);
//		}
//		for (long id : termIds) {
//			aspifLongIdentifier.append(",").append(id);
//		}

		int aspifValue;
		if (this.aspifMap.containsKey(aspifLongIdentifier)) {
			aspifValue = aspifMap.get(aspifLongIdentifier);
		} else {
			aspifValue = this.aspifCounter++;
			this.aspifMap.put(aspifLongIdentifier, aspifValue);
		}
		return negated ? -aspifValue : aspifValue;
	}

	/**
	 * Get the predicate index in the list of approximated predicates.
	 *
	 * @param predicate the predicate
	 * @return the index
	 */
	private long getPredicateIndex(Predicate predicate) {
		return this.approximatedPredicatesList.indexOf(predicate);
	}

	/**
	 * Get the term ids for a literal and a grounding.
	 *
	 * @param literal the literal
	 * @param answerMap the map representing the grounding
	 * @return the term ids
	 */
	private long[] getTermIds(Literal literal, Map<Variable, Long> answerMap) {
		long[] termIds = new long[literal.getPredicate().getArity()];
		int idx = 0;
		for (Term term : literal.getArguments()) {
			if (term.isVariable()) {
				termIds[idx] = answerMap.get(term);
			} else {
				try {
					termIds[idx] = this.reasoner.getConstantId(term.getName());
				} catch (NotStartedException e) {
					System.out.println(e.getMessage());
				}
			}
			idx++;
		}
		return termIds;
	}

	/**
	 * Get the term ids for a fact.
	 *
	 * @param fact the fact
	 * @return the term ids
	 */
	private long[] getTermIds(Fact fact) {
		long[] termIds = new long[fact.getPredicate().getArity()];
		int idx = 0;
		for (Term term : fact.getArguments()) {
			if (term.isConstant()) {
				// Facts should not contain variables
				try {
					termIds[idx] = this.reasoner.getConstantId(term.getName());
				} catch (NotStartedException e) {
					System.out.println(e.getMessage());
				}
			}
			idx++;
		}
		return termIds;
	}

	private long[] getTermIds(List<Integer> integerList) {
		long[] termIds = new long[integerList.size()];
		for (int i=0; i < integerList.size(); i++) {
			termIds[i] = integerList.get(i);
		}
		return termIds;
	}

	/**
	 * Returns for a variable the corresponding term with respect to a partial answer. If no term is found, the
	 * variable is kept.
	 *
	 * @param variable the variable
	 * @param answerMap the partial answer
	 * @return the variable or the term to which the variable is mapped
	 */
	private Term getTermFromPartialAnswer(Variable variable, Map<Variable, Long> answerMap) {
		Long termId;
		if ((termId = answerMap.get(variable)) != null) {
			try {
				karmaresearch.vlog.Term term;
				String s = reasoner.getConstant(termId);
				if (s == null) {
					term = new karmaresearch.vlog.Term(TermType.BLANK, "" + (termId >> 40) + "_"
						+ ((termId >> 32) & 0377) + "_" + (termId & 0xffffffffL));
				} else {
					term = new karmaresearch.vlog.Term(TermType.CONSTANT, s);
				}
				return reasoner.toTerm(term);
			} catch (NotStartedException e) {
				// Should not happen, we just did a query ...
				return variable;
			}
		} else {
			return variable;
		}
	}
}
