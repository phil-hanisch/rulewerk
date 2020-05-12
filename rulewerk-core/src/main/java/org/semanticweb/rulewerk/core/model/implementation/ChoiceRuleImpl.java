package org.semanticweb.rulewerk.core.model.implementation;

import java.util.Set;
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
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.model.api.Conjunction;
import org.semanticweb.rulewerk.core.model.api.Literal;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.ChoiceRule;
import org.semanticweb.rulewerk.core.model.api.ChoiceElement;
import org.semanticweb.rulewerk.core.model.api.StatementVisitor;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.model.api.UniversalVariable;

/**
 * Implementation for {@link ChoiceRule}. Represents asp choice rule.
 * 
 * @author Philipp Hanisch
 *
 */
public class ChoiceRuleImpl implements ChoiceRule {

	final Conjunction<Literal> body;
	final List<ChoiceElement> head;
	
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
	public ChoiceRuleImpl(final List<ChoiceElement> head, final Conjunction<Literal> body) {
		Validate.notNull(head);
		Validate.notNull(body);
		Validate.notEmpty(head,
				"Empty rule head not supported. To capture integrity constraints, use a dedicated predicate that represents a contradiction.");
		if (body.getExistentialVariables().count() > 0) {
			throw new IllegalArgumentException(
					"Rule body cannot contain existential variables. Rule was: " + head + " :- " + body);
		}
		Set<UniversalVariable> bodyVariables = body.getUniversalVariables().collect(Collectors.toSet());
		// if (head.getUniversalVariables().filter(x -> !bodyVariables.contains(x)).count() > 0) {
		// 	throw new IllegalArgumentException(
		// 			"Universally quantified variables in rule head must also occur in rule body. Rule was: " + head
		// 					+ " :- " + body);
		// }

		this.head = head;
		this.body = body;
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
	public Conjunction<PositiveLiteral> getHead() {
		throw new UnsupportedOperationException("The head of choice rules are special");
	}

	@Override
	public Conjunction<Literal> getBody() {
		return this.body;
	}

	@Override
	public List<ChoiceElement> getChoiceElements() {
		return this.head;
	};


	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}

	@Override
	public Stream<Term> getTerms() {
		return Stream.concat(this.body.getTerms(), this.head.stream().flatMap(elem -> elem.getTerms())).distinct();
	}

}
