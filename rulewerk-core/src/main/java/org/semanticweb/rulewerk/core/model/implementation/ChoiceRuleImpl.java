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

import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.model.api.*;

/**
 * Implementation for {@link ChoiceRule}. Represents asp choice rule.
 *
 * @author Philipp Hanisch
 *
 */
public class ChoiceRuleImpl implements ChoiceRule {
	final Conjunction<Literal> body;
	final List<ChoiceElement> head;
	final Integer upperBound;
	final Integer lowerBound;
	final int ruleIdx;

	/**
	 * Creates a Rule with a (possibly empty) body and an non-empty head. All variables in
	 * the body must be universally quantified; all variables in the head that do
	 * not occur in the body must be existentially quantified.
	 * @param head list of choice elements representing the rule
	 *             head conjuncts.
	 * @param body list of Literals (negated or non-negated) representing the rule
	 * @param lowerBound the lower bound of head elements to choose
	 * @param upperBound the upper bound of head elements to choose
	 * @param ruleIdx the rule index
	 */
	public ChoiceRuleImpl(final List<ChoiceElement> head, final Conjunction<Literal> body, Integer lowerBound, Integer upperBound, final int ruleIdx) {
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
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = this.body.hashCode();
		result = prime * result + this.head.hashCode();
		result = prime * result + lowerBound;
		result = prime * result + upperBound;
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

		return this.head.equals(other.getChoiceElements()) && this.body.equals(other.getBody())
			&& this.lowerBound.equals(other.getLowerBound()) && this.upperBound.equals(other.getUpperBound());
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
	public Integer getUpperBound() {
		return upperBound;
	}

	@Override
	public Integer getLowerBound() {
		return lowerBound;
	}

	@Override
	public Boolean hasUpperBound() {
		return getLowerBound().equals(Integer.MAX_VALUE);
	}

	@Override
	public Boolean hasLowerBound() {
		return lowerBound <= 0;
	}

	@Override
	public List<Rule> getApproximation(Set<Predicate> approximatedPredicates) {
		List<Rule> list = new ArrayList<>();

		// add helper rule for grounding global variables
		PositiveLiteral literal = this.getHelperLiteral();
		Conjunction<PositiveLiteral> head = new ConjunctionImpl<>(Collections.singletonList(literal));
		list.add(new RuleImpl(head, this.body.getSimplifiedConjunction(approximatedPredicates, true)));

		// add helper rule for grounding local variables (based on global variables)
		int i = 0;
		for (ChoiceElement choiceElement : this.head) {
			Conjunction<Literal> context = choiceElement.getContext();
			List<Term> terms = Stream.concat(this.body.getUniversalVariables(), context.getUniversalVariables()).distinct().collect(Collectors.toList());
			literal = this.getHelperLiteral(terms, this.ruleIdx, i);
			head = new ConjunctionImpl<>(Collections.singletonList(literal));

			List<Literal> bodyLiterals = new ArrayList<>(this.body.getLiterals());
			bodyLiterals.addAll(context.getLiterals());

			list.add(new RuleImpl(head, (new ConjunctionImpl<>(bodyLiterals)).getSimplifiedConjunction(approximatedPredicates, true)));
			list.add(new RuleImpl(new ConjunctionImpl<>(Collections.singletonList(choiceElement.getLiteral())), new ConjunctionImpl<>(head.getLiterals())));

			i++;
		}

		return list;
	}

	@Override
	public String ground(Set<Predicate> approximatedPredicates, Map<Variable, Term> answerMap) {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		for (int i=0; i<this.getChoiceElements().size(); i++) {
			if (i != 0) {
				builder.append("; ");
			}
			builder.append("%").append(i + 1).append("$s");
		}
		builder.append(" }");

		builder.append(" :- ");
		builder.append(this.body.ground(approximatedPredicates, answerMap));
		builder.append(" .\n");
		return builder.toString();
	}

	@Override
	public String groundAspif(Set<Predicate> approximatedPredicates, AspifIndex aspifIndex, Map<Variable, Term> answerMap) {
		return null;
	}

	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}

	@Override
	public Stream<Term> getTerms() {
		return Stream.concat(this.body.getTerms(), this.head.stream().flatMap(ChoiceElement::getTerms)).distinct();
	}

	@Override
	public <T> T accept(AspRuleVisitor<T> aspRuleVisitor) {
		return aspRuleVisitor.visit(this);
	}
}
