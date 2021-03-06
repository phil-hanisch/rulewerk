package org.semanticweb.rulewerk.core.model.api;

import org.semanticweb.rulewerk.core.model.implementation.PositiveLiteralImpl;
import org.semanticweb.rulewerk.core.model.implementation.PredicateImpl;
import org.semanticweb.rulewerk.core.model.implementation.Serializer;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;

import java.io.FileWriter;
import java.util.List;
import java.util.Map;
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

/**
 * Interface for classes representing an asp rule. This implementation assumes that
 * rules are defined by their head and body literals, without explicitly
 * specifying quantifiers. All variables in the body are considered universally
 * quantified; all variables in the head that do not occur in the body are
 * considered existentially quantified.
 *
 * @author Philipp Hanisch
 *
 */
public abstract interface AspRule extends SyntaxObject, Statement, Entity {

	/**
	 * Returns the conjunction of head literals (the consequence of the rule).
	 *
	 * @return conjunction of literals
	 */
	Conjunction<PositiveLiteral> getHeadLiterals();

	/**
	 * Returns the conjunction of body literals (the premise of the rule).
	 *
	 * @return conjunction of literals
	 */
	Conjunction<Literal> getBody();

	/**
	 * Returns true if the rule can only be approximated
	 *
	 * @return whether the rule requires approximation
	 */
	boolean requiresApproximation();

	/**
	 * Returns a list of rules that approximate the (asp) rule by plain datalog rules
	 * @param approximatedPredicates set of approximated predicates
	 *
	 * @return list of rules
	 */
	List<Rule> getApproximation(Set<Predicate> approximatedPredicates);

	/**
	 * Accept a {@link AspRuleVisitor} and return its output.
	 *
	 * @param aspRuleVisitor the AspRuleVisitor
	 * @return output of the visitor
	 */
	<T> T accept(AspRuleVisitor<T> aspRuleVisitor);

	/**
	 * Get the grounding of the rule for the given instance
	 *
	 * @param approximatedPredicates set of approximated predicates
	 * @param answerMap contains a term for each variable
	 * @return the grounding of the rule
	 */
	String ground(Set<Predicate> approximatedPredicates, Map<Variable, Term> answerMap);

	/**
	 * Get the unique rule index
	 *
	 * @return the rule index
	 */
	int getRuleIdx();

	@Override
	default String getSyntacticRepresentation() {
		return Serializer.getString(this);
	}

	/**
	 * Construct the helper literal that is unique for this rule and has the same variables as the rule body
	 *
	 * @return a positive helper literal
	 */
	default PositiveLiteral getHelperLiteral() {
		String predicateName = "rule" + this.getRuleIdx();
		List<Term> terms = this.getBody().getUniversalVariables().collect(Collectors.toList());
		Predicate predicate = new PredicateImpl(predicateName, terms.size());
		return new PositiveLiteralImpl(predicate, terms);
	}

	/**
	 * Construct a helper literal based on the given terms and indices
	 *
	 * @param terms the relevant variables
	 * @param indices a list of indices
	 * @return a positive helper literal
	 */
	default PositiveLiteral getHelperLiteral(List<Term> terms, int... indices) {
		StringBuilder predicateName = new StringBuilder("rule");
		boolean first = true;
		for (int i : indices) {
			if (first) {
				first = false;
			} else {
				predicateName.append("A");
			}
			predicateName.append(i);
		}
		Predicate predicate = new PredicateImpl(predicateName.toString(), terms.size());
		return new PositiveLiteralImpl(predicate, terms);
	}
}
