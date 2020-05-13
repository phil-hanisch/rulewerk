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
import java.util.ArrayList;
import java.util.Arrays;

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
		Set<UniversalVariable> bodyVariables = body.getUniversalVariables().collect(Collectors.toSet());
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
		return new ConjunctionImpl<PositiveLiteral>(literals);
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
	public List<Rule> getApproximation() {
		List<Rule> list = new ArrayList<Rule>();

		// add helper rule for grounding global variables
		String predicateName = "rule_" + this.ruleIdx;
		List<Term> terms = this.body.getUniversalVariables().collect(Collectors.toList());
		Predicate predicate = new PredicateImpl(predicateName, terms.size());
		PositiveLiteral literal = new PositiveLiteralImpl(predicate, terms);
		Conjunction<PositiveLiteral> head = new ConjunctionImpl(Arrays.asList(literal));
		list.add(new RuleImpl(head, this.body));

		// add helper rule for grounding local variables (based on global variables)
		int i = 0;
		for (ChoiceElement choiceElement : this.head) {
			Conjunction<Literal> context = choiceElement.getContext();

			predicateName = "rule_" + this.ruleIdx + "_" + i;
			terms = Stream.concat(this.body.getUniversalVariables(), context.getUniversalVariables()).distinct().collect(Collectors.toList());
			predicate = new PredicateImpl(predicateName, terms.size());
			literal = new PositiveLiteralImpl(predicate, terms);
			head = new ConjunctionImpl(Arrays.asList(literal));

			Conjunction<Literal> body = new ConjunctionImpl(this.body, context);
			list.add(new RuleImpl(head, body));
			list.add(new RuleImpl(new ConjunctionImpl(Arrays.asList(choiceElement.getLiteral())), new ConjunctionImpl(head.getLiterals())));

			i++;
		}

		return list;
	}

	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}

	@Override
	public Stream<Term> getTerms() {
		return Stream.concat(this.body.getTerms(), this.head.stream().flatMap(elem -> elem.getTerms())).distinct();
	}

}
