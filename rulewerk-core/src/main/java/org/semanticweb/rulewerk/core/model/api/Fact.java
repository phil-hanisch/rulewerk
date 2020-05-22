package org.semanticweb.rulewerk.core.model.api;

import org.semanticweb.rulewerk.core.model.implementation.Serializer;

import java.util.Map;

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
 * A fact is a positive (non-negated) literal that contains only constants as
 * its terms, but no variables.
 *
 * @author Markus Kroetzsch
 *
 */
public interface Fact extends PositiveLiteral, Statement {

	@Override
	default String getSyntacticRepresentation() {
		return Serializer.getFactString(this);
	}

	/**
	 * Get the identifier for aspif, same for the negated and non-negated literal of same predicate
	 *
	 * @return the aspif identifier
	 */
	default String getAspifIdentifier() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getPredicate().getSyntacticRepresentation()).append("(");
		for (Term term : this.getArguments()) {
			builder.append(term.getSyntacticRepresentation());
			builder.append(",");
		}
		builder.append(")");
		return builder.toString();
	};
}
