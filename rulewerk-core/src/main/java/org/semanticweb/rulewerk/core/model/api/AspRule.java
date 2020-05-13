package org.semanticweb.rulewerk.core.model.api;

import org.semanticweb.rulewerk.core.model.implementation.Serializer;
import java.util.List;

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
	 * Returns true if the rule is a choice rule.
	 *
	 * @return whether the rule is a choice rule
	 */
	boolean isChoiceRule();

	/**
	 * Returns true if the rule can only be approximated
	 *
	 * @return whether the rule requires approximation
	 */
	boolean requiresApproximation();

	/**
	 * Returns a list of rules that approximate the (asp) rule by plain datalog rules
	 *
	 * @return list of rules 
	 */
	List<Rule> getApproximation();

	@Override
	default String getSyntacticRepresentation() {
		return Serializer.getString(this);
	}
}
