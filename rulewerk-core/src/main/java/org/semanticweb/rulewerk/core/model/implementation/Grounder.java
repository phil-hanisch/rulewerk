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
import org.semanticweb.rulewerk.core.model.api.*;
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

	final private Set<Predicate> approximatedPredicates;
	final private Reasoner reasoner;
	final private BufferedWriter writer;
	final private boolean textFormat;
	final private AspifIndex aspifIndex;

	/**
	 * The constructor.
	 *
	 * @param reasoner the reasoner with the information for the grounding
	 * @param writer a file writer for writing the grounded rules
	 * @param approximatedPredicates set of approximated predicates
	 * @param textFormat determines if the grounding format is textual
	 */
	public Grounder(Reasoner reasoner, BufferedWriter writer, Set<Predicate> approximatedPredicates, boolean textFormat) {
		this.reasoner = reasoner;
		this.writer = writer;
		this.approximatedPredicates = approximatedPredicates;
		this.textFormat = textFormat;
		this.aspifIndex = new AspifIndexImpl(reasoner);
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
	 * Grounds an asp rule (constraint or disjunctive rule) and writes it via the file writer
	 *
	 * @param disjunctiveRule whether the rule is a disjunctive rule
	 * @param rule the rule to ground
	 */
	public void groundRule(AspRule rule, boolean disjunctiveRule) throws IOException {
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
		System.out.println("Timing for rule " + rule.getSyntacticRepresentation());
		System.out.println("Duration: " + ((endTime - startTime) / 1000000) + " ms");
		System.out.println("Instances: " + counter);
		if (counter > 0) {
			System.out.println("Duration per instance: " + ((endTime - startTime) / counter ) + " ns");
		}
		System.out.println();
	}

	/**
	 * ground an asp choice rule and write via the file writer
	 *
	 * @param rule the rule to ground
	 */
	public void groundRule(ChoiceRule rule) throws IOException {
		PositiveLiteral literal = rule.getHelperLiteral();
		Map<Variable, Long> answerMap = new HashMap<>();
		List<Variable> variables = literal.getUniversalVariables().collect(Collectors.toList());

		int counter = 0;
		try (final karmaresearch.vlog.QueryResultIterator answers = reasoner.answerQueryInNativeFormat(literal, true)) {
			// each query result represents a grounding (= grounding of the global variables)
			while (answers.hasNext()) {
				counter++;
				long[] terms = answers.next();
				for (int i = 0; i < terms.length; i++) {
					answerMap.put(variables.get(i), terms[i]);
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
					// helper integer for body (get and write)
					int bodyHelpInteger = 1; // = aspifIndex.getAspifInteger(literal, answerMap);
					writer.write("1 0 1 " + bodyHelpInteger); // rule statement for disjunctive rule with a head literal
					writeNormalBodyAspif(rule.getBody(), answerMap);

					Set<Integer> choiceElementToCountIntegers = new HashSet<>();
					int idx = 0;
					for (ChoiceElement choiceElement : rule.getChoiceElements()) {
						choiceElementToCountIntegers.addAll(writeAndCollectChoiceElementAspif(choiceElement, rule, answerMap, idx, bodyHelpInteger));
						idx++;
					}

					// if there are bounds, take care that they are satisfied
					if (rule.hasLowerBound()) {
						// introduce integer to check if enough elements has been chosen
						int lowerBoundInteger = 1; // = aspifIndex.getAspifInteger(literal, answerMap, 0);
						writer.write("1 0 1 " + lowerBoundInteger); // rule statement for a disjunctive rule with a single head literal
						writer.write(" 1 " + rule.getLowerBound() + " " + choiceElementToCountIntegers.size()); // weighted body
						for (Integer choiceElementToCount : choiceElementToCountIntegers) {
							writer.write(" " + choiceElementToCount + " 1"); // element with weight 1
						}
						writer.write("\n");

						writer.write("1 0 0 0 2 " + bodyHelpInteger + " -" + lowerBoundInteger);
						writer.write("\n");
					}
					if (rule.hasUpperBound()) {
						// introduce integer to check if too many elements has been chosen
						int upperBoundInteger = 1; // aspifIndex.getAspifInteger(literal, answerMap, 1);
						writer.write("1 0 1 " + upperBoundInteger); // rule statement for a disjunctive rule with a single head literal
						writer.write(" 1 " + (rule.getUpperBound() + 1) + " " + choiceElementToCountIntegers.size()); // weighted body
						for (Integer choiceElementToCount : choiceElementToCountIntegers) {
							writer.write(" " + choiceElementToCount + " 1"); // element with weight 1
						}
						writer.write("\n");

						writer.write("1 0 0 0 2 " + bodyHelpInteger + " " + upperBoundInteger);
						writer.write("\n");
					}
//				}
			}
		}
		System.out.println(counter);
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
				writer.write(" " + 1); // aspifIndex.getAspifInteger(literal, answerMap));
			}
		} else {
			writer.write(" 0"); // #headLiteral = 0
		}

		writeNormalBodyAspif(rule.getBody(), answerMap);
	}

	/**
	 * Write the aspif instances of the choice rule that allows to choose a grounding of the choice element if the body
	 * and the condition is satisfied. Introduce and collect an integer for each grounding that represents that this
	 * grounding is chosen, i.e. the integer for the grounding and the condition are true.
	 * Remark: The introduced integer is the same for two groundings iff the grounded literals are the same and they
	 * belong to the same rule, i.e. the condition does not matter.
	 *
	 * @param choiceElement the choice element to ground
	 * @param rule the rule the choice element belongs to
	 * @param globalMap a map representing the body instance
	 * @param idx the index of the choice element (in the head)
	 * @param bodyHelpInteger an integer that is true iff all literals of the body are true
	 * @return the integer set
	 */
	private Set<Integer> writeAndCollectChoiceElementAspif(ChoiceElement choiceElement, ChoiceRule rule, Map<Variable, Long> globalMap, int idx, int bodyHelpInteger) {
		// Get all the variables and terms used by the body and the condition of the choice element
		// For the variables: Replace them with the constant if they are part of the grounding of the body
		List<Term> terms = Stream.concat(rule.getBody().getUniversalVariables(), choiceElement.getContext().getUniversalVariables())
			.distinct()
			.map(variable -> {
				Long termId;
				if ((termId = globalMap.get(variable)) != null) {
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
			})
			.collect(Collectors.toList());

		Map<Variable, Long> map = new HashMap<>(globalMap);
		PositiveLiteral literal = rule.getHelperLiteral(terms, rule.getRuleIdx(), idx);
		final karmaresearch.vlog.QueryResultIterator answers = reasoner.answerQueryInNativeFormat(literal, true);

		Set<Integer> choiceElementToCountIntegerSet = new HashSet<>();
		while (answers.hasNext()) {
			// build the map that represents the completely (locally and globally) ground rule
			long[] localTerms = answers.next();
			for (int i = 0; i < terms.size(); i++) {
				Term globalTerm = terms.get(i);
				if (globalTerm.isVariable()) {
					map.put((Variable) globalTerm, localTerms[i]);
				}
			}

			try {
				int choiceElementInteger = aspifIndex.getAspifInteger(choiceElement.getLiteral(), map);
				// choice element integer :- body integer, condition integers
				writer.write("1 1 1"); // rule statement for a choice rule for a single literal
				writer.write(" " + choiceElementInteger);
				// TODO: Consider introducing helper literal for the condition
				writer.write(" 0 " + (choiceElement.getContext().getRelevantLiteralCount(approximatedPredicates) + 1));
				writeConjunctionAspif(choiceElement.getContext(), map);
				writer.write(" " + bodyHelpInteger);
				writer.write("\n");

				if (rule.hasLowerBound() || rule.hasUpperBound()) {
					int choiceElementToCountInteger = aspifIndex.getAspifInteger(literal, map, rule.getRuleIdx());
					// choice element counts integer :- choice element integer, condition integers
					writer.write("1 0 1 " + choiceElementToCountInteger); // rule statement for a disjunctive rule with a single head literal
					writer.write(" 0 " + (choiceElement.getContext().getRelevantLiteralCount(approximatedPredicates) + 1));
					writeConjunctionAspif(choiceElement.getContext(), map);
					writer.write(" " + choiceElementInteger);
					writer.write("\n");

					// collect element counts integer
					choiceElementToCountIntegerSet.add(choiceElementToCountInteger);
				}

			} catch (IOException e) {
				System.out.println("An error occurred.");
				e.printStackTrace();
			}
		}

		return choiceElementToCountIntegerSet;
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
				writer.write(" " +  aspifIndex.getAspifInteger(literal, answerMap));
			}
		}
	}

	/**
	 * Write the given fact in aspif
	 *
	 * @param fact the fact to write
	 */
	public void writeFactAspif(Fact fact) {
		String aspifFact = "1 0 1 " + aspifIndex.getAspifInteger(fact) + " 0 0\n";
		try {
			writer.write(aspifFact);
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
}
