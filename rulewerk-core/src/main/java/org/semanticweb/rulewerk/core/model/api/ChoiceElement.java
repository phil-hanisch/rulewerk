package org.semanticweb.rulewerk.core.model.api;

import org.semanticweb.rulewerk.core.model.implementation.Serializer;

import java.util.Map;
import java.util.Set;

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
public interface ChoiceElement extends SyntaxObject, Entity {

	/**
	 * The "head" literal.
	 *
	 * @return the literal
	 */
	PositiveLiteral getLiteral();

	/**
	 * The list of literals defining the context for the "head" literal.
	 *
	 * @return a list of literals.
	 */
	Conjunction<Literal> getContext();

	@Override
	default String getSyntacticRepresentation() {
		return Serializer.getString(this);
	}

	/**
	 * Return the instance for the choice element based on the map
	 *
	 * @param approximatedPredicates a set of approximated predicates
	 * @param map contains a term for each variable
	 * @return the grounded choice element
	 */
    String ground(Set<Predicate> approximatedPredicates, Map<Variable, Term> map);
}
