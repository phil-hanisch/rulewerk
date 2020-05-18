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

	/**
	 * The constructor.
	 *
	 * @param reasoner the reasoner with the information for the grounding
	 * @param writer a file writer for writing the grounded rules
	 * @param approximatedPredicates set of approximated predicates
	 */
	public Grounder(Reasoner reasoner, FileWriter writer, Set<Predicate> approximatedPredicates) {
		this.reasoner = reasoner;
		this.writer = writer;
		this.approximatedPredicates = approximatedPredicates;
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
		Map<Variable, Term> answerMap = new HashMap<>();
		List<Variable> variables = literal.getUniversalVariables().collect(Collectors.toList());

		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
			// each query result represents a grounding
			while(answers.hasNext()) {
				List<Term> terms = answers.next().getTerms();
				for (int i = 0; i < variables.size(); i++) {
					answerMap.put(variables.get(i), terms.get(i));
				}

				String groundedRule = rule.ground(approximatedPredicates, answerMap);
				try {
					writer.write(groundedRule);
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * ground an asp choice rule and write via the file writer
	 *
	 * @param rule the rule to ground
	 */
	public void groundRule(ChoiceRule rule) {
		PositiveLiteral literal = rule.getHelperLiteral();
		Map<Variable, Term> answerMap = new HashMap<>();
		List<Variable> variables = literal.getUniversalVariables().collect(Collectors.toList());

		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
			// each query result represents a grounding (= grounding of the global variables)
			while (answers.hasNext()) {
				List<Term> terms = answers.next().getTerms();
				for (int i = 0; i < variables.size(); i++) {
					answerMap.put(variables.get(i), terms.get(i));
				}

				// ground choice with placeholder for choice elements
				String groundedRule = rule.ground(approximatedPredicates, answerMap);

				Object[] choiceElements = new String[rule.getChoiceElements().size()];
				int idx = 0;
				// each choice element contributes individual grounded elements
				for (ChoiceElement choiceElement : rule.getChoiceElements()) {
					choiceElements[idx] = groundChoiceElement(choiceElement, rule, answerMap, idx);
					idx++;
				}

				try {
					writer.write(String.format(groundedRule, choiceElements));
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}
			}
		}
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
}
