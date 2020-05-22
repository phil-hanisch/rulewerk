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

import org.semanticweb.rulewerk.core.model.implementation.Serializer;

/**
 * Interface for literals. A positive literal is simply an atomic formula, i.e.,
 * a formula of the form P(t1,...,tn) where P is a {@link Predicate} of arity n
 * and t1,...,tn are {@link Term}s. A negative literal is a negated atomic
 * formula.
 *
 * @author david.carral@tu-dresden.de
 * @author Irina Dragoste
 */
public interface Literal extends SyntaxObject, Entity {

	boolean isNegated();

	/**
	 * The literal predicate.
	 *
	 * @return the literal predicate.
	 */
	Predicate getPredicate();

	/**
	 * The list of terms representing the tuple arguments.
	 *
	 * @return an unmodifiable list of terms with the same size as the
	 *         {@link Predicate} arity.
	 */
	List<Term> getArguments();

	@Override
	default String getSyntacticRepresentation() {
		return Serializer.getString(this);
	}

	/**
	 * Get the identifier for aspif, same for the negated and non-negated literal of same predicate
	 *
	 * @return the aspif identifier
	 */
	default String getAspifIdentifier(Map<Variable, Term> map) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getPredicate().getSyntacticRepresentation()).append("(");
		for (Term term : this.getArguments()) {
			if (term.isVariable()) {
				builder.append(map.get(term).getSyntacticRepresentation());
			} else {
				builder.append(term.getSyntacticRepresentation());
			}
			builder.append(",");
		}
		builder.append(")");
		return  builder.toString();
	};
}
