package org.semanticweb.rulewerk.core.model.implementation;

import java.util.*;

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
import org.semanticweb.rulewerk.core.model.api.*;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;

/**
 * Implementation for {@link Disjunctive}. Represents a disjunctive asp rule.
 *
 * @author Philipp Hanisch
 *
 */
public class DisjunctiveRuleImpl implements DisjunctiveRule {

	final Conjunction<Literal> body;
	final Conjunction<PositiveLiteral> head;
	final int ruleIdx;

	/**
	 * Creates a Rule with a (possibly empty) body and an non-empty head. All variables in
	 * the body must be universally quantified; all variables in the head that do
	 * not occur in the body must be existentially quantified.
	 *
	 * @param head list of positive literals (non-negated) representing the rule
	 *             head conjuncts.
	 * @param body list of Literals (negated or non-negated) representing the rule
	 *             body conjuncts.
	 */
	public DisjunctiveRuleImpl(final Conjunction<PositiveLiteral> head, final Conjunction<Literal> body, final int ruleIdx) {
		Validate.notNull(head);
		Validate.notNull(body);
		Validate.notEmpty(head.getLiterals(),
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
		System.out.println(this);
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
		if (!(obj instanceof DisjunctiveRule)) {
			return false;
		}
		final DisjunctiveRule other = (DisjunctiveRule) obj;

		return this.head.equals(other.getHeadLiterals()) && this.body.equals(other.getBody());
	}

	@Override
	public String toString() {
		return getSyntacticRepresentation();
	}

	@Override
	public Conjunction<PositiveLiteral> getHeadLiterals() {
		return this.head;
	}

	@Override
	public Conjunction<Literal> getBody() {
		return this.body;
	}

	@Override
	public int getRuleIdx() {
		return this.ruleIdx;
	}

	@Override
	public List<Rule> getApproximation(Set<Predicate> approximatedPredicates) {
		PositiveLiteral literal = this.getHelperLiteral();
		List<Rule> list = new ArrayList<>();
		list.add(new RuleImpl(new ConjunctionImpl<>(Collections.singletonList(literal)), this.body.getSimplifiedConjunction(approximatedPredicates, true)));
		list.add(new RuleImpl(this.head, new ConjunctionImpl<>(Collections.singletonList(literal))));

		return list;
	}

	@Override
	public boolean requiresApproximation() {
		return this.head.getLiterals().size() > 1;
	}

	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}

	@Override
	public Stream<Term> getTerms() {
		return Stream.concat(this.body.getTerms(), this.head.getTerms()).distinct();
	}

	@Override
	public String ground(Set<Predicate> approximatedPredicates, Map<Variable, Term> answerMap) {
		String body = this.body.ground(approximatedPredicates, answerMap);
		String head = this.head.getSyntacticRepresentation(answerMap);
		return head + " :- " + body + " .\n";
	}

	@Override
	public String groundAspif(Set<Predicate> approximatedPredicates, AspifIndex aspifIndex, Map<Variable, Long> answerMap) {
		StringBuilder builder = new StringBuilder();
		builder.append(1); // rule statement
		builder.append(" ").append(0); // rule type == disjunctive
		builder.append(" ").append(this.getHeadLiterals().getLiterals().size()); // #headLiterals
		for (Literal literal : this.getHeadLiterals().getLiterals()) {
			builder.append(" ").append(aspifIndex.getAspifInteger(literal, answerMap));
		}
		this.appendBodyAspif(builder, approximatedPredicates, aspifIndex, answerMap);
		return builder.toString();
	}

	@Override
	public <T> T accept(AspRuleVisitor<T> aspRuleVisitor) {
		return aspRuleVisitor.visit(this);
	}
}
