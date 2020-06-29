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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

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

/**
 * Class for grounding asp rules and facts. The grounder uses a file writer and a reasoner that has the (asp) facts
 * materialized. Moreover, the grounder utilizes the knowledge about approximated predicates to omit facts that are
 * certainly true. The grounder creates, on-the-fly, an index containing the integers of the grounded literals.
 */
public class Grounder implements AspRuleVisitor<Boolean> {

	final static int TOP_CONSTANT = 0;

	final private KnowledgeBase knowledgeBase;
	final private Set<Predicate> approximatedPredicates;
	final private Reasoner reasoner;
	final private BufferedWriter writer;
	final private boolean textFormat;
	final private Object2IntMap<String> aspifMap;
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
		this.textFormat = textFormat;
		this.aspifMap = new Object2IntOpenHashMap<>();
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

			this.knowledgeBase.getAspRules().forEach(rule -> rule.accept(this));

			// TODO: Ground show statements
		} else {
			try {
				this.writer.write("asp 1 0 0");
				this.writer.newLine();

				this.knowledgeBase.getFacts().forEach(this::writeFactAspif);
				this.knowledgeBase.getAspRules().forEach(rule -> rule.accept(this));
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

		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
			// each query result represents a grounding
			while(answers.hasNext()) {
				List<Term> terms = answers.next().getTerms();
				try {
					writeShowStatementAspif(literal, terms);
				} catch (IOException e) {
					e.printStackTrace();
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
			Map<Variable, Term> answerMap = new HashMap<>();
			final List<Variable> variables = literal.getUniversalVariables().collect(Collectors.toList());

			long startTime = System.nanoTime();
			int counter = 0;

			try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
				// each query result represents a grounding
				while(answers.hasNext()) {
					counter++;
					final List<Term> terms = answers.next().getTerms();
					for (int i = 0; i < terms.size(); i++) {
						answerMap.put(variables.get(i), terms.get(i));
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

		Map<Variable, Term> answerMapHeadVariables = new HashMap<>();
		int counter = 0;
		try (final QueryResultIterator answersBody = reasoner.answerQuery(bodyLiteral, true)) {
			// each query result represents a grounding (= grounding of the global variables)

			while (answersBody.hasNext()) {
				counter++;
				List<Term> terms = answersBody.next().getTerms();
				for (int i = 0; i < terms.size(); i++) {
					answerMapHeadVariables.put(relevantGlobalVariables.get(i), terms.get(i));
				}

				// helper integer for body (get and write)
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
								int countLiteralInteger = getAspifValue();

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
				if (rule.hasLowerBound() || rule.hasUpperBound()) {
					long lowerBoundInteger = 0;
					long upperBoundInteger = 0;
					long boundInteger;


					// get integer to check if enough elements has been chosen, if there is a lower bound
					if (rule.hasLowerBound()) {
						lowerBoundInteger = getAspifValue();
						writer.write("1 0 1 " + lowerBoundInteger // rule statement for a disjunctive rule with a single head literal
							+ " 1 " + rule.getLowerBound() + " " + chosenLiteralSet.size() + " " // weighted body
							+ StringUtils.join(chosenLiteralSet, " 1 ") + " 1" // elements with weight 1
						);
						writer.newLine();
					}

					// get integer to check if too many elements has been chosen, if there is an upper bound
					if (rule.hasUpperBound()) {
						upperBoundInteger = getAspifValue();
						writer.write("1 0 1 " + upperBoundInteger // rule statement for a disjunctive rule with a single head literal
							+ " 1 " + (rule.getUpperBound() + 1) + " " + chosenLiteralSet.size() + " " // weighted body
							+ StringUtils.join(chosenLiteralSet, " 1 ") + " 1" // elements with weight 1
						);
						writer.newLine();
					}

					// get the right integer corresponding if the bounds are satisfied
					if (rule.hasLowerBound() && rule.hasUpperBound()) {
						boundInteger = getAspifValue();
						writer.write("1 0 1 " + boundInteger // rule statement for a disjunctive rule with a single head literal
							+ " 0 2 " + lowerBoundInteger + " -" + upperBoundInteger // normal body with the two bounds
						);
						writer.newLine();
					} else if (rule.hasLowerBound()) {
						boundInteger = -lowerBoundInteger;
					} else {
						boundInteger = upperBoundInteger;
					}

					// ensure that the bounds are satisfied if the body is satisfied
					if (bodyHelpInteger > 0) {
						writer.write("1 0 0 0 2 " + bodyHelpInteger + " -" + boundInteger);
					} else {
						writer.write("1 0 0 0 1 -" + boundInteger);
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
	private int getAndWriteBodyHelpInteger(ChoiceRule rule, PositiveLiteral bodyLiteral, Map<Variable, Term> answerMapHeadVariables) throws IOException {
		int approximatedLiteralCount = rule.getBody().getRelevantLiteralCount(approximatedPredicates);
		if (approximatedLiteralCount == 0) {
			return TOP_CONSTANT;
		} else if (approximatedLiteralCount == 1 && rule.getBodyOnlyGlobalVariables().count() == 0) {
			Literal literal = rule.getBody().getLiterals().stream().filter(literal1 -> approximatedPredicates.contains(literal1.getPredicate())).findFirst().orElse(bodyLiteral);
			String aspifIdentifier = getAspifIdentifier(literal, answerMapHeadVariables);
			return getAspifValue(aspifIdentifier, literal.isNegated());
		} else {
			int bodyHelpInteger = getAspifValue();

			List<Term> partiallyGroundedVariables = rule.getGlobalVariables()
				.map(variable -> answerMapHeadVariables.getOrDefault(variable, variable))
				.collect(Collectors.toList());
			Map<Variable, Term> answerMapBodyVariables = new HashMap<>(answerMapHeadVariables);
			PositiveLiteral bodyAllLiteral = rule.getHelperLiteral("bodyAll", partiallyGroundedVariables, rule.getRuleIdx());

			try (final QueryResultIterator answersBodyAll = reasoner.answerQuery(bodyAllLiteral, true)) {

				while (answersBodyAll.hasNext()) {
					// build the map that represents the completely (locally and globally) ground rule
					List<Term> allTerms = answersBodyAll.next().getTerms();
					for (int i = 0; i < partiallyGroundedVariables.size(); i++) {
						Term globalTerm = partiallyGroundedVariables.get(i);
						if (globalTerm.isVariable()) {
							answerMapBodyVariables.put((Variable) globalTerm, allTerms.get(i));
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
	private void writeRuleInstanceAspif(AspRule rule, Map<Variable, Term> answerMap, boolean disjunctiveRule) throws IOException {
		writer.write("1 0"); // rule statement for a disjunctive rule
		if (disjunctiveRule) {
			writer.write(" " + rule.getHeadLiterals().getLiterals().size()); // #headLiterals
			for (Literal literal : rule.getHeadLiterals().getLiterals()) {
				final String aspifIdentifier = getAspifIdentifier(literal, answerMap);
				writer.write(" " + getAspifValue(aspifIdentifier, literal.isNegated()));
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
	private void addChoiceElementAspifToMap(Map<Integer, List<Integer>> choiceSetMap, ChoiceElement choiceElement, ChoiceRule rule, Map<Variable, Term> answerMapHeadVariables, int idx) throws IOException {
		// Get all the variables and terms used by the body and the condition of the choice element
		// For the variables: Replace them with the constant if they are part of the grounding of the body

		List<Term> terms = Stream.concat(
			rule.getBody().getUniversalVariables(),
			choiceElement.getContext().getUniversalVariables()
		).distinct().map(
			variable -> answerMapHeadVariables.getOrDefault(variable, variable)
		).collect(Collectors.toList());

		Map<Variable, Term> answerMapChoiceElement = new HashMap<>(answerMapHeadVariables);
		PositiveLiteral literal = rule.getHelperLiteral(terms, rule.getRuleIdx(), idx);
		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {

			while (answers.hasNext()) {
				// build the map that represents the completely (locally and globally) ground rule
				List<Term> localTerms = answers.next().getTerms();
				for (int i = 0; i < terms.size(); i++) {
					Term term = terms.get(i);
					if (term instanceof Variable) {
						answerMapChoiceElement.put((Variable) term, localTerms.get(i));
					}
				}

				// get the integer for the literal of the choice element
				String aspifIdentifier = getAspifIdentifier(choiceElement.getLiteral(), answerMapChoiceElement);
				int literalInteger = getAspifValue(aspifIdentifier, false);

				// get the integers for the literals of the condition of the choice element
				// ignore not approximated literals
				List<Integer> conditionIntegerList = choiceElement.getContext().getLiterals().stream().filter(
					literal1 -> approximatedPredicates.contains(literal1.getPredicate())
				).map(
					literal1 -> getAspifValue(getAspifIdentifier(literal1, answerMapChoiceElement), literal1.isNegated())
				).collect(Collectors.toList());
				// an empty list has to be handled differently
				int conditionInteger = conditionIntegerList.isEmpty()
					? TOP_CONSTANT
					: getAspifValue();

				if (choiceSetMap.containsKey(conditionInteger)) {
					choiceSetMap.get(conditionInteger).add(literalInteger);
				} else {
					if (conditionInteger != TOP_CONSTANT) {
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
	private void writeNormalBodyAspif(Conjunction<Literal> body, Map<Variable, Term> answerMap) throws IOException {
		writer.write(" 0 " // normal body
			+ body.getRelevantLiteralCount(approximatedPredicates));
		writeConjunctionAspif(body, answerMap);
		writer.newLine();
	}

	/**
	 * Write the integers representing the literals of the conjunction for the instance given by the answerMap.
	 * Ignore literals that are not approximated, as they are always true.
	 * @param conjunction the conjunction to get the aspif integers for
	 * @param answerMap a map representing the instance of the conjunction
	 * @throws IOException an exception due to writing to a file
	 */
	private void writeConjunctionAspif(Conjunction<Literal> conjunction, Map<Variable, Term> answerMap) throws IOException {
		for (Literal literal : conjunction.getLiterals()) {
			if (approximatedPredicates.contains(literal.getPredicate())) {
				final String aspifIdentifier = getAspifIdentifier(literal, answerMap);
				writer.write(" " +  getAspifValue(aspifIdentifier, literal.isNegated()));
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
			final String aspifIdentifier = getAspifIdentifier(fact, fact.getArguments());
			try {
				writer.write("1 0 1 " + getAspifValue(aspifIdentifier, false) + " 0 0");
				writer.newLine();
			} catch (IOException e) {
				System.out.println("An error occurred.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Write the instance of a show statement.
	 *
	 * @param literal the literal to show
	 * @param terms the terms for the literal
	 * @throws IOException due to writing to file
	 */
	public void writeShowStatementAspif(PositiveLiteral literal, List<Term> terms) throws IOException {
		StringBuilder symbolicRepresentation = new StringBuilder();
		symbolicRepresentation.append(literal.getPredicate().getName());
		boolean firstConstant = true;
		for (Term term : terms) {
			if (firstConstant) {
				symbolicRepresentation.append("(");
				firstConstant = false;
			} else {
				symbolicRepresentation.append(",");
			}

			symbolicRepresentation.append(term.getName());
		}
		symbolicRepresentation.append(")");

		writer.write("4 " // show statement
			+ symbolicRepresentation.length() + " "
			+ symbolicRepresentation.toString());

		if (approximatedPredicates.contains(literal.getPredicate())) {
			writer.write(" 1 " + getAspifValue(getAspifIdentifier(literal, terms), false));
		} else {
			writer.write(" 0");
		}

		writer.newLine();
	}

//	/**
//	 * Get the integer that represents a ground literal.
//	 *
//	 * @param predicateId the predicate id
//	 * @param negated whether the literal is negated
//	 * @param termIds array of term ids
//	 * @param context context in which the literal is used
//	 * @return the aspif integer
//	 */
//	private int getAspifValue(long predicateId, boolean negated, long[] termIds, long... context) {
//		long aspifLongIdentifier = 0;
//		long base = 1 + Math.max(numberOfConstants, numberOfRules);
//		for (long id : context) {
//			aspifLongIdentifier = aspifLongIdentifier * base + id + 1;
//		}
//		for (long id : termIds) {
//			aspifLongIdentifier = aspifLongIdentifier * base + id + 1;
//		}
//		aspifLongIdentifier = aspifLongIdentifier * (numberOfPredicates + numberOfRules) + predicateId;
////		StringBuilder aspifLongIdentifier = new StringBuilder();
////		aspifLongIdentifier.append(predicateId);
////		for (long id : context) {
////			aspifLongIdentifier.append(":").append(id);
////		}
////		for (long id : termIds) {
////			aspifLongIdentifier.append(",").append(id);
////		}
//
//		int aspifValue;
//		if (this.aspifMap.containsKey(aspifLongIdentifier)) {
//			aspifValue = aspifMap.get(aspifLongIdentifier);
//		} else {
//			aspifValue = this.aspifCounter++;
//			this.aspifMap.put(aspifLongIdentifier, aspifValue);
//		}
//		return negated ? -aspifValue : aspifValue;
//	}

	/**
	 * Returns a String that identifies the literal grounded with the answer map
	 * @param literal the literal
	 * @param answerMap the map containing the answer mapping
	 *
	 * @return the string identifier
	 */
	private String getAspifIdentifier(Literal literal, Map<Variable, Term> answerMap) {
		StringBuilder builder = new StringBuilder();
		builder.append(literal.getPredicate().getName());
		for (Term term : literal.getArguments()) {
			Term groundedTerm = answerMap.getOrDefault(term, term);
			builder.append("_").append(groundedTerm.getName());
		}
		return builder.toString();
	}

	/**
	 * Returns a String that identifies the literal grounded with the answer map
	 * @param literal the literal
	 * @param answerTerms list of terms
	 *
	 * @return the string identifier
	 */
	private String getAspifIdentifier(Literal literal, List<Term> answerTerms) {
		StringBuilder builder = new StringBuilder();
		builder.append(literal.getPredicate().getName());
		for (Term term : answerTerms) {
			builder.append("_").append(term.getName());
		}
		return builder.toString();
	}

	/**
	 * Get the integer that represents a ground literal.
	 *
	 * @param aspifIdentifier the string identifier for the literal
	 * @param negated whether the literal is negated
	 * @return the aspif integer
	 */
	private int getAspifValue(String aspifIdentifier, boolean negated) {
		int aspifValue = aspifMap.getOrDefault(aspifIdentifier, 0);
		if (aspifValue == 0) {
			aspifValue = this.aspifCounter++;
			this.aspifMap.put(aspifIdentifier, aspifValue);
		}

		return negated ? -aspifValue : aspifValue;
	}

	/**
	 * Get a one-time only aspif integer
	 *
	 * @return an aspif integer
	 */
	private int getAspifValue() {
		return aspifCounter++;
	}
}
