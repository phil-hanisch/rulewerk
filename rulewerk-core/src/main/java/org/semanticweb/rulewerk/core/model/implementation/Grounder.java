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

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Grounder implements AspRuleVisitor<Boolean> {

	final private Set<Predicate> approximatedPredicates;
	final private Reasoner reasoner;
	final private FileWriter writer;
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
	public Grounder(Reasoner reasoner, FileWriter writer, Set<Predicate> approximatedPredicates, boolean textFormat) {
		this.reasoner = reasoner;
		this.writer = writer;
		this.approximatedPredicates = approximatedPredicates;
		this.textFormat = textFormat;
		this.aspifIndex = new AspifIndexImpl(reasoner);
	}

	@Override
	public Boolean visit(ChoiceRule rule) {
		this.groundRule(rule);
		return true;
	}

	@Override
	public Boolean visit(Constraint rule) {
		this.groundRule(rule);
		return true;
	}

	@Override
	public Boolean visit(DisjunctiveRule rule) {
		this.groundRule(rule);
		return true;
	}

	/**
	 * ground an asp rule and write via the file writer
	 *
	 * @param rule the rule to ground
	 */
	public void groundRule(AspRule rule) {
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
				String groundedRule;

				for (int i = 0; i < terms.length; i++) {
					answerMap.put(variables.get(i), terms[i]);
				}

//				if (this.textFormat) {
//					groundedRule = rule.ground(approximatedPredicates, answerMap);
//				} else {
				groundedRule = rule.groundAspif(approximatedPredicates, aspifIndex, answerMap);
//				}

				try {
					writer.write(groundedRule);
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}
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
	public void groundRule(ChoiceRule rule) {
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
					StringBuilder builder = new StringBuilder();
					// rule statement; with disjunctive head; with one literal
					builder.append(1).append(" ").append(0).append(" ").append(1);
					Integer bodyHelpInteger = aspifIndex.getAspifInteger(literal, answerMap);
					builder.append(" ").append(bodyHelpInteger);
					rule.appendBodyAspif(builder, approximatedPredicates, aspifIndex, answerMap);
					try {
						writer.write(builder.toString());
					} catch (IOException e) {
						System.out.println("An error occurred.");
						e.printStackTrace();
					}

					Set<Integer> choiceElementIntegerSet = new HashSet<>();
					int idx = 0;
					for (ChoiceElement choiceElement : rule.getChoiceElements()) {
						addChoiceElementIntegers(choiceElementIntegerSet, choiceElement, rule, answerMap, idx);
						idx++;
					}

					builder = new StringBuilder(11 + 2 * choiceElementIntegerSet.size());
					// rule statement; choice rule
					builder.append(1).append(" ").append(1);
					builder.append(" ").append(choiceElementIntegerSet.size());
					for (Integer integer : choiceElementIntegerSet) {
						builder.append(" ").append(integer);
					}
					// normal body; with one literal; the helper literal
					builder.append(" ").append(0).append(" ").append(1).append(" ").append(bodyHelpInteger).append("\n");
					try {
						writer.write(builder.toString());
					} catch (IOException e) {
						System.out.println("An error occurred.");
						e.printStackTrace();
					}

					// handle constraints
					// -- element counts :- element, context
					// -- lower bound satisfied (p_lower)
					// -- upper bound satisfied (p_upper)
					// -- bound satisfied (p_bound :- p_lower, not p_upper)
					// -- :- body, not p_bound
//				}
			}
		}
		System.out.println(counter);
	}

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

	public void addChoiceElementIntegers(Set<Integer> integerSet, ChoiceElement choiceElement, ChoiceRule rule, Map<Variable, Long> globalMap, int idx) {
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

		while (answers.hasNext()) {
			long[] localTerms = answers.next();
			for (int i = 0; i < terms.size(); i++) {
				Term globalTerm = terms.get(i);
				if (globalTerm.isVariable()) {
					map.put((Variable) globalTerm, localTerms[i]);
				}
			}

			integerSet.add(aspifIndex.getAspifInteger(choiceElement.getLiteral(), map));
		}
	}

	/**
	 * Write the fact in aspif
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
