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
	private int[][][] headMapping;
	private int[][][] bodyMapping;

	/**
	 * This class is used as light-weight collection of elements that uniquely identifies (grounded) literals for
	 * aspif groundings. Based on these identifiers, the class provides statically the functionality to get an integer
	 * that is on-the-fly uniquely connected with a certain aspif identifier.
	 */
	 static class AspifIdentifier {

		final static private Object2IntMap<AspifIdentifier> aspifMap = new Object2IntOpenHashMap<>();
		static private int counter = 1;

		final private String predicateName;
		final private String[] termNames;

		/**
		 * Constructor. Create an aspif identifier for the given literal and the list of terms as its arguments.
		 *
		 * @param literal the literal
		 * @param answerTerms the arguments
		 */
		public AspifIdentifier(Literal literal, List<Term> answerTerms) {
			this.predicateName = literal.getPredicate().getName();

			this.termNames = new String[answerTerms.size()];
			int i = 0;
			for (Term term : answerTerms) {
				this.termNames[i++] = term.getName();
			}
		}

		/**
		 * Constructor. Create an aspif identifier for the given literal. Its arguments are constructed based on the
		 * answer terms and an integer array that specify which of the answer terms should be used at each position.
		 *
		 * @param literal the literal
		 * @param answerTerms the terms from an answer
		 * @param mapping integer array
		 */
		public AspifIdentifier(Literal literal, List<Term> answerTerms, int[] mapping) {
			this.predicateName = literal.getPredicate().getName();

			this.termNames = new String[mapping.length];
			for (int i=0; i < mapping.length; i++) {
				int index = mapping[i];
				if (index == -1) {
					this.termNames[i] = literal.getArguments().get(i).getName();
				} else {
					this.termNames[i] = answerTerms.get(index).getName();
				}
			}
		}

		public String[] getTermNames() {
			return termNames;
		}

		public String getPredicateName() {
			return predicateName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int hashcode = 23;
			hashcode = hashcode * prime + this.predicateName.hashCode();
			hashcode = hashcode * prime + Arrays.hashCode(termNames);
			return hashcode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof AspifIdentifier)) {
				return false;
			}
			final AspifIdentifier other = (AspifIdentifier) obj;
			return this.predicateName.equals(other.getPredicateName())
				&& Arrays.equals(this.termNames, other.getTermNames());
		}

		/**
		 * Get and possibly negate the aspif integer for the given aspif identifier.
		 *
		 * @param aspifIdentifier the aspif identifier for a grounded literal
		 * @param negated whether the literal is negated
		 * @return the aspif integer
		 */
		public static int getAspifValue(AspifIdentifier aspifIdentifier, boolean negated) {
			int aspifValue = AspifIdentifier.aspifMap.getOrDefault(aspifIdentifier, 0);
			if (aspifValue == 0) {
				aspifValue = AspifIdentifier.counter++;
				AspifIdentifier.aspifMap.put(aspifIdentifier, aspifValue);
			}

			return negated ? -aspifValue : aspifValue;
		}

		/**
		 * Get a one-time only aspif integer that can be used to abbreviate constructs.
		 *
		 * @return an aspif integer
		 */
		public static int getAspifValue() {
			return AspifIdentifier.counter++;
		}
	}

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
			List<Term> globalVariables = rule.getGlobalVariables().collect(Collectors.toList());
			this.headMapping = getHeadVariableMapping(rule);
			this.bodyMapping = getBodyVariableMapping(rule.getBody(), rule.getHelperLiteral("bodyAll", globalVariables, rule.getRuleIdx()));
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
			this.headMapping = new int[0][0][0];
			this.bodyMapping = getBodyVariableMapping(rule.getBody(), rule.getHelperLiteral());
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
			this.headMapping = getHeadVariableMapping(rule, rule.getHelperLiteral());
			this.bodyMapping = getBodyVariableMapping(rule.getBody(), rule.getHelperLiteral());
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
			final List<Variable> variables = literal.getUniversalVariables().collect(Collectors.toList());

			long startTime = System.nanoTime();
			int counter = 0;

			try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
				// each query result represents a grounding
				while(answers.hasNext()) {
					counter++;
					List<Term> answerTerms = answers.next().getTerms();

	//				if (this.textFormat) {
	//					groundedRule = rule.ground(approximatedPredicates, answerMap);
	//					try {
	//						writer.write(groundedRule);
	//					} catch (IOException e) {
	//						System.out.println("An error occurred.");
	//						e.printStackTrace();
	//					}
	//				} else {
					writeRuleInstanceAspif(rule, answerTerms, disjunctiveRule);
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

		int counter = 0;
		try (final QueryResultIterator answersBody = reasoner.answerQuery(bodyLiteral, true)) {
			// each query result represents a grounding (= grounding of the global variables)

			while (answersBody.hasNext()) {
				counter++;
				List<Term> globalTerms = answersBody.next().getTerms();

				// helper integer for body (get and write)
				int bodyHelpInteger = getAndWriteBodyHelpInteger(rule, bodyLiteral, globalTerms);

				Map<Integer, List<Integer>> choiceSetMap = new HashMap<>();
				int idx = 0;
				for (ChoiceElement choiceElement : rule.getChoiceElements()) {
					addChoiceElementAspifToMap(choiceSetMap, choiceElement, rule, globalTerms, idx);
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
								int countLiteralInteger = AspifIdentifier.getAspifValue();

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
						lowerBoundInteger = AspifIdentifier.getAspifValue();
						writer.write("1 0 1 " + lowerBoundInteger // rule statement for a disjunctive rule with a single head literal
							+ " 1 " + rule.getLowerBound() + " " + chosenLiteralSet.size() + " " // weighted body
							+ StringUtils.join(chosenLiteralSet, " 1 ") + (chosenLiteralSet.size() > 0 ? " 1" : "") // elements with weight 1
						);
						writer.newLine();
					}

					// get integer to check if too many elements has been chosen, if there is an upper bound
					if (rule.hasUpperBound()) {
						upperBoundInteger = AspifIdentifier.getAspifValue();
						writer.write("1 0 1 " + upperBoundInteger // rule statement for a disjunctive rule with a single head literal
							+ " 1 " + (rule.getUpperBound() + 1) + " " + chosenLiteralSet.size() + " " // weighted body
							+ StringUtils.join(chosenLiteralSet, " 1 ") + (chosenLiteralSet.size() > 0 ? " 1" : "") // elements with weight 1
						);
						writer.newLine();
					}

					// get the right integer corresponding if the bounds are satisfied
					if (rule.hasLowerBound() && rule.hasUpperBound()) {
						boundInteger = AspifIdentifier.getAspifValue();
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
	private int getAndWriteBodyHelpInteger(ChoiceRule rule, PositiveLiteral bodyLiteral, List<Term> globalAnswerTerms) throws IOException {
		int approximatedLiteralCount = rule.getBody().getRelevantLiteralCount(approximatedPredicates);
		if (approximatedLiteralCount == 0) {
			return TOP_CONSTANT;
		} else if (approximatedLiteralCount == 1 && rule.getBodyOnlyGlobalVariables().count() == 0) {
			Literal literal = rule.getBody().getLiterals().stream().filter(literal1 -> approximatedPredicates.contains(literal1.getPredicate())).findFirst().orElse(bodyLiteral);
			int index = rule.getBody().getLiterals().indexOf(literal);
			final AspifIdentifier aspifIdentifier = new AspifIdentifier(literal, globalAnswerTerms, bodyMapping[index][0]);
			return AspifIdentifier.getAspifValue(aspifIdentifier, literal.isNegated());
		} else {
			int bodyHelpInteger = AspifIdentifier.getAspifValue();

			List<Term> partiallyGroundedVariables = new ArrayList<>(globalAnswerTerms);
			partiallyGroundedVariables.addAll(rule.getBodyOnlyGlobalVariables().collect(Collectors.toList()));
			PositiveLiteral bodyAllLiteral = rule.getHelperLiteral("bodyAll", partiallyGroundedVariables, rule.getRuleIdx());

			try (final QueryResultIterator answersBodyAll = reasoner.answerQuery(bodyAllLiteral, true)) {

				while (answersBodyAll.hasNext()) {
					// build the map that represents the completely (locally and globally) ground rule
					List<Term> allTerms = answersBodyAll.next().getTerms();

					writer.write("1 0 1 " + // rule statement for disjunctive rule with single head literal
						bodyHelpInteger // for the body help integer
					);
					writeNormalBodyAspif(rule.getBody(), allTerms);
				}
			}

			return bodyHelpInteger;
		}
	}

	/**
	 * Return a three-dimensional integer array that tells for each conditional literal of the rule head and for each
	 * literal of such an conditional literal at which position the corresponding term for a grounding of the literal,
	 * which can be computed by evaluating the helper literal, is.
	 * Remarks: If the term is not present in the helper literal, e.g. because it is a constant, a -1 is added to the
	 * integer array. As disjunctive rules do not (yet) support conditional literals the "normal" literals are regarded
	 * as conditional literals with an empty condition.
	 *
	 * @param rule
	 * @param helperLiteral
	 *
	 * @return
	 */
	private int[][][] getHeadVariableMapping(DisjunctiveRule rule, PositiveLiteral helperLiteral) {
		List<PositiveLiteral> literals = rule.getHeadLiterals().getLiterals();
		int[][][] headMapping = new int[literals.size()][][];
		int i = 0;
		for (PositiveLiteral literal : literals) {
			headMapping[i] = new int[1][];
			headMapping[i][0] = getLiteralVariableMapping(literal, helperLiteral);
		}
		return headMapping;
	}

	/**
	 * Return a three-dimensional integer array that tells for each choice element of the rule head and for each
	 * literal of such a choice element at which position the corresponding term for a grounding of the literal,
	 * which can be computed by evaluating the helper literal, is.
	 * Remarks: If the term is not present in the helper literal, e.g. because it is a constant, a -1 is added to the
	 * integer array.
	 *
	 * @param rule
	 *
	 * @return
	 */
	private int[][][] getHeadVariableMapping(ChoiceRule rule) {
		List<ChoiceElement> choiceElements = rule.getChoiceElements();
		int[][][] headMapping = new int[choiceElements.size()][][];
		int counterChoiceElements = 0;
		for (ChoiceElement choiceElement : choiceElements) {
			List<Literal> literals = new ArrayList<>();
			literals.add(choiceElement.getLiteral());
			literals.addAll(choiceElement.getContext().getLiterals());

			List<Term> choiceElementVariables = Stream.concat(
				rule.getRelevantGlobalVariables(),
				choiceElement.getContext().getUniversalVariables()
			).distinct().collect(Collectors.toList());
			PositiveLiteral helperLiteral = rule.getHelperLiteral(choiceElementVariables, rule.getRuleIdx(), counterChoiceElements);

			headMapping[counterChoiceElements] = new int[literals.size()][];

			int counterLiterals = 0;
			for (Literal literal : literals) {
				headMapping[counterChoiceElements][counterLiterals] = getLiteralVariableMapping(literal, helperLiteral);
				counterLiterals++;
			}

			counterChoiceElements++;
		}
		return headMapping;
	}

	/**
	 * Return a three-dimensional integer array that tells for each conditional literal of the rule body and for each
	 * literal of such an conditional literal at which position the corresponding term for a grounding of the literal,
	 * which can be computed by evaluating the helper literal, is.
	 * Remarks: If the term is not present in the helper literal, e.g. because it is a constant, a -1 is added to the
	 * integer array. As rule bodies do not (yet) support conditional literals the "normal" literals are regarded
	 * as conditional literals with an empty condition.
	 *
	 * @param rule
	 * @param helperLiteral
	 *
	 * @return
	 */
	private int[][][] getBodyVariableMapping(Conjunction<Literal> body, PositiveLiteral helperLiteral) {
		List<Literal> literals = body.getLiterals();
		int[][][] bodyMapping = new int[literals.size()][][];
		int i = 0;
		for (Literal literal : literals) {
			bodyMapping[i] = new int[1][];
			bodyMapping[i][0] = getLiteralVariableMapping(literal, helperLiteral);
			i++;
		}
		return bodyMapping;
	}

	/**
	 * Return an integer array that tells for each term of the given literal at which position the corresponding term
	 * for a grounding of the literal, which can be computed by evaluating the helper literal, is. If the term is not
	 * present in the helper literal, e.g. because it is a constant, a -1 is added to the integer array.
	 *
	 * @param literal the literal
	 * @param helperLiteral the helper literal
	 * @return integer array
	 */
	private int[] getLiteralVariableMapping(Literal literal, PositiveLiteral helperLiteral) {
		List<Term> terms = literal.getArguments();
		int[] mapping = new int[terms.size()];
		int i = 0;
		for (Term term : terms) {
			mapping[i++] = helperLiteral.getArguments().indexOf(term);
		}
		return mapping;
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

	private void writeRuleInstanceAspif(AspRule rule, List<Term> answerTerms, boolean disjunctiveRule) throws IOException {
		writer.write("1 0"); // rule statement for a disjunctive rule
		if (disjunctiveRule) {
			writer.write(" " + rule.getHeadLiterals().getLiterals().size()); // #headLiterals
			int i = 0;
			for (Literal literal : rule.getHeadLiterals().getLiterals()) {
				int[] mapping = headMapping[i][0];
				final AspifIdentifier aspifIdentifier = new AspifIdentifier(literal, answerTerms, mapping);
				writer.write(" " + AspifIdentifier.getAspifValue(aspifIdentifier, literal.isNegated()));
				i++;
			}
		} else {
			writer.write(" 0"); // #headLiteral = 0
		}

		writeNormalBodyAspif(rule.getBody(), answerTerms);
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
	private void addChoiceElementAspifToMap(Map<Integer, List<Integer>> choiceSetMap, ChoiceElement choiceElement, ChoiceRule rule, List<Term> globalAnswerTerms, int idx) throws IOException {
		// Get all the variables and terms used by the body and the condition of the choice element
		// For the variables: Replace them with the constant if they are part of the grounding of the body

		List<UniversalVariable> globalVariables = rule.getGlobalVariables().collect(Collectors.toList());
		List<Term> partiallyGroundedVariables = new ArrayList<>(globalAnswerTerms);
		partiallyGroundedVariables.addAll(choiceElement.getContext().getUniversalVariables().filter(var -> !globalVariables.contains(var)).collect(Collectors.toList()));

		PositiveLiteral literal = rule.getHelperLiteral(partiallyGroundedVariables, rule.getRuleIdx(), idx);
		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {

			while (answers.hasNext()) {
				// build the map that represents the completely (locally and globally) ground rule
				List<Term> answerTerms = answers.next().getTerms();

				// get the integer for the literal of the choice element
				AspifIdentifier aspifIdentifier = new AspifIdentifier(choiceElement.getLiteral(), answerTerms, headMapping[idx][0]);
				int literalInteger = AspifIdentifier.getAspifValue(aspifIdentifier, false);

				List<Integer> conditionIntegerList = new ArrayList<>();
				int countLiteral = 0;
				for (Literal conditionLiteral : choiceElement.getContext().getLiterals()) {
					if (approximatedPredicates.contains(conditionLiteral.getPredicate())) {
						AspifIdentifier aspifIdentifierConditionLiteral = new AspifIdentifier(conditionLiteral, answerTerms, headMapping[idx][countLiteral+1]);
						conditionIntegerList.add(AspifIdentifier.getAspifValue(aspifIdentifierConditionLiteral, conditionLiteral.isNegated()));
					}

					countLiteral++;
				}

				// an empty list has to be handled differently
				int conditionInteger = conditionIntegerList.isEmpty()
					? TOP_CONSTANT
					: AspifIdentifier.getAspifValue();

				if (choiceSetMap.containsKey(conditionInteger)) {
					choiceSetMap.get(conditionInteger).add(literalInteger);
				} else {
					if (conditionInteger != TOP_CONSTANT) {
						writer.write("1 0 1 " + conditionInteger); // rule statement for a disjunctive rule with a single literal
						writeConditionAspif(choiceElement.getContext(), answerTerms, idx);
					}
					choiceSetMap.put(conditionInteger, new ArrayList<>(Collections.singletonList(literalInteger)));
				}
			}

		}
	}

	/**
	 * Write the body instance given by the answer
	 *
	 * @param body a conjunction of literals
	 * @param answerTerms list of terms representing the grounding
	 * @throws IOException
	 */
	private void writeNormalBodyAspif(Conjunction<Literal> body, List<Term> answerTerms) throws IOException {
		StringBuilder builder = new StringBuilder();
		int counter = 0;
		int i = 0;

		for (Literal literal : body.getLiterals()) {
			if (approximatedPredicates.contains(literal.getPredicate())) {
				int[] mapping = bodyMapping[i][0];
				final AspifIdentifier aspifIdentifier = new AspifIdentifier(literal, answerTerms, mapping);
				builder.append(" ").append(AspifIdentifier.getAspifValue(aspifIdentifier, literal.isNegated()));
				counter++;
			}
			i++;
		}

		writer.write(" 0 " // normal body
			+ counter
			+ builder.toString());
		writer.newLine();
	}

	private void writeConditionAspif(Conjunction<Literal> body, List<Term> answerTerms, int choiceElementIndex) throws IOException {
		StringBuilder builder = new StringBuilder();
		int counter = 0;
		int i = 0;

		for (Literal literal : body.getLiterals()) {
			if (approximatedPredicates.contains(literal.getPredicate())) {
				int[] mapping = headMapping[choiceElementIndex][i+1];
				final AspifIdentifier aspifIdentifier = new AspifIdentifier(literal, answerTerms, mapping);
				builder.append(" ").append(AspifIdentifier.getAspifValue(aspifIdentifier, literal.isNegated()));
				counter++;
			}
			i++;
		}

		writer.write(" 0 " // normal body
			+ counter
			+ builder.toString());
		writer.newLine();
	}

	/**
	 * Write the given fact in aspif
	 *
	 * @param fact the fact to write
	 */
	public void writeFactAspif(Fact fact) {
		if (approximatedPredicates.contains(fact.getPredicate())) {
			final AspifIdentifier aspifIdentifier = new AspifIdentifier(fact, fact.getArguments());
			try {
				writer.write("1 0 1 " + AspifIdentifier.getAspifValue(aspifIdentifier, false) + " 0 0");
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
			writer.write(" 1 " + AspifIdentifier.getAspifValue(new AspifIdentifier(literal, terms), false));
		} else {
			writer.write(" 0");
		}

		writer.newLine();
	}
}
