package org.semanticweb.rulewerk.core.model.api;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.rulewerk.core.model.implementation.Serializer;

/**
 * Interface for representing conjunctions of {@link Literal}s, i.e., lists of
 * (negated or positive) atomic formulas that are connected with logical AND.
 * Conjunctions may have free variables, since they contain no quantifiers.
 *
 * @author Markus Krötzsch
 *
 */
public interface Conjunction<T extends Literal> extends Iterable<T>, SyntaxObject, Entity {

	/**
	 * Returns the list of literals that are part of this conjunction.
	 *
	 * @return list of literals
	 */
	List<T> getLiterals();

	/**
	 * Returns a conjunction without literals build from the given predicates
	 * @param predicates set of predicates to remove from conjunction
	 * @param keepPositive whether to keep the non-negated literals
	 *
	 * @return conjunction
	 */
	Conjunction<T> getSimplifiedConjunction(Set<Predicate> predicates, boolean keepPositive);

	@Override
	default String getSyntacticRepresentation() {
		return Serializer.getString(this);
	}

	String ground(Set<Predicate> approximatedPredicates, Map<Variable, Term> answerMap);
}
