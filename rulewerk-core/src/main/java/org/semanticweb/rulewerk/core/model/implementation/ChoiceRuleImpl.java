package org.semanticweb.rulewerk.core.model.implementation;

import java.util.*;
import java.util.stream.Collectors;

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

import java.util.stream.Stream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.model.api.Conjunction;
import org.semanticweb.rulewerk.core.model.api.Literal;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.ChoiceRule;
import org.semanticweb.rulewerk.core.model.api.ChoiceElement;
import org.semanticweb.rulewerk.core.model.api.StatementVisitor;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.model.api.UniversalVariable;
import org.semanticweb.rulewerk.core.model.api.QueryResult;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;

/**
 * Implementation for {@link ChoiceRule}. Represents asp choice rule.
 *
 * @author Philipp Hanisch
 *
 */
public class ChoiceRuleImpl implements ChoiceRule {

	final Conjunction<Literal> body;
	final List<ChoiceElement> head;
	final int ruleIdx;

	/**
	 * Creates a Rule with a (possibly empty) body and an non-empty head. All variables in
	 * the body must be universally quantified; all variables in the head that do
	 * not occur in the body must be existentially quantified.
	 *
	 * @param head list of choice elements representing the rule
	 *             head conjuncts.
	 * @param body list of Literals (negated or non-negated) representing the rule
	 *             body conjuncts.
	 */
	public ChoiceRuleImpl(final List<ChoiceElement> head, final Conjunction<Literal> body, final int ruleIdx) {
		Validate.notNull(head);
		Validate.notNull(body);
		Validate.notEmpty(head,
				"Empty rule head not supported. To capture integrity constraints, use a dedicated predicate that represents a contradiction.");
		if (body.getExistentialVariables().count() > 0) {
			throw new IllegalArgumentException(
					"Rule body cannot contain existential variables. Rule was: " + head + " :- " + body);
		}
		// Set<UniversalVariable> bodyVariables = body.getUniversalVariables().collect(Collectors.toSet());
		// if (head.getUniversalVariables().filter(x -> !bodyVariables.contains(x)).count() > 0) {
		// 	throw new IllegalArgumentException(
		// 			"Universally quantified variables in rule head must also occur in rule body. Rule was: " + head
		// 					+ " :- " + body);
		// }

		this.head = head;
		this.body = body;
		this.ruleIdx = ruleIdx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = this.body.hashCode();
		result = prime * result + this.head.hashCode();
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ChoiceRule)) {
			return false;
		}
		final ChoiceRule other = (ChoiceRule) obj;

		return this.head.equals(other.getChoiceElements()) && this.body.equals(other.getBody());
	}

	@Override
	public String toString() {
		return getSyntacticRepresentation();
	}

	@Override
	public Conjunction<PositiveLiteral> getHeadLiterals() {
		List<PositiveLiteral> literals = this.getChoiceElements().stream().map(ChoiceElement::getLiteral).collect(Collectors.toList());
		return new ConjunctionImpl<>(literals);
	}

	@Override
	public Conjunction<Literal> getBody() {
		return this.body;
	}

	@Override
	public List<ChoiceElement> getChoiceElements() {
		return this.head;
	}

	@Override
	public int getRuleIdx() {
		return this.ruleIdx;
	}

	@Override
	public List<Rule> getApproximation() {
		List<Rule> list = new ArrayList<>();

		// add helper rule for grounding global variables
		PositiveLiteral literal = this.getHelperLiteral();
		Conjunction<PositiveLiteral> head = new ConjunctionImpl<>(Collections.singletonList(literal));
		list.add(new RuleImpl(head, this.body));

		// add helper rule for grounding local variables (based on global variables)
		int i = 0;
		for (ChoiceElement choiceElement : this.head) {
			Conjunction<Literal> context = choiceElement.getContext();
			List<Term> terms = Stream.concat(this.body.getUniversalVariables(), context.getUniversalVariables()).distinct().collect(Collectors.toList());
			literal = this.getHelperLiteral(terms, this.ruleIdx, i);
			head = new ConjunctionImpl<>(Collections.singletonList(literal));

			List<Literal> bodyLiterals = new ArrayList<>(this.body.getLiterals());
			bodyLiterals.addAll(context.getLiterals());

			list.add(new RuleImpl(head, new ConjunctionImpl<>(bodyLiterals)));
			list.add(new RuleImpl(new ConjunctionImpl<>(Collections.singletonList(choiceElement.getLiteral())), new ConjunctionImpl<>(head.getLiterals())));

			i++;
		}

		return list;
	}

	@Override
	public void groundRule(Reasoner reasoner, Set<Predicate> approximatedPredicates, FileWriter writer) {
		List<Term> terms = this.body.getUniversalVariables().collect(Collectors.toList());
		PositiveLiteral literal = getHelperLiteral();

		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
			// each query result represents a grounding (= grounding of the global variables)
			while(answers.hasNext()) {
				QueryResult answer = answers.next();
				String[] answerTerms = answer.getTerms().stream().map(Term::getSyntacticRepresentation).toArray(String[]::new);

				StringBuilder builder = new StringBuilder();
				builder.append("{ ");
				boolean first = true;
				int i = 0;
				// each choice element contributes individual grounded elements
				for (ChoiceElement choiceElement : this.head) {
					List<Term> contextTerms = Stream.concat(answer.getTerms().stream(), choiceElement.getContext().getUniversalVariables().filter(variable -> !terms.contains(variable)))
													.distinct().collect(Collectors.toList());
					Predicate predicate = new PredicateImpl("rule_" + this.ruleIdx + "_" + i, contextTerms.size());
					literal = new PositiveLiteralImpl(predicate, contextTerms);
					final QueryResultIterator elementAnswers = reasoner.answerQuery(literal, true);

					String elementTemplate = choiceElement.getLiteral().getSyntacticRepresentation() +
						" : " +
						choiceElement.getContext().getSyntacticRepresentation();

					// each query result represents a grounded choice element in the head (= grounding of the local variables)
					while(elementAnswers.hasNext()) {
						String[] elementAnswerTerms = elementAnswers.next().getTerms().stream().map(Term::getSyntacticRepresentation).toArray(String[]::new);

						if (first) {
							first = false;
						} else {
							builder.append("; ");
						}

						Iterator<UniversalVariable> iterator = Stream.concat(this.getBody().getUniversalVariables(), choiceElement.getContext().getUniversalVariables()).distinct().iterator();
						int argIdx = 1;
						while (iterator.hasNext()) {
							elementTemplate = elementTemplate.replaceAll(iterator.next().getSyntacticRepresentation().replaceAll("\\?", "\\\\?"), "\\%" + argIdx + "\\$s");
							argIdx++;
						}

						builder.append(String.format(elementTemplate, elementAnswerTerms));
					}
				}

				builder.append(" }");

				// replace variable names in the body with placeholders
				String bodyTemplate = this.getBodyTemplate(approximatedPredicates);
				Iterator<UniversalVariable> iterator = this.body.getUniversalVariables().iterator();
				int argIdx = 1;
				while (iterator.hasNext()) {
					bodyTemplate = bodyTemplate.replaceAll(iterator.next().getSyntacticRepresentation().replaceAll("\\?", "\\\\?"), "\\%" + argIdx + "\\$s");
					argIdx++;
				}
				builder.append(String.format(bodyTemplate, answerTerms));

				builder.append(" .\n");
				try {
					writer.write(builder.toString());
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}

	@Override
	public Stream<Term> getTerms() {
		return Stream.concat(this.body.getTerms(), this.head.stream().flatMap(ChoiceElement::getTerms)).distinct();
	}
}
