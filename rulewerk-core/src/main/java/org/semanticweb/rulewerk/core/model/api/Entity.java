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

import java.util.Map;

/**
 * Interface for every parsable data model that has a string representation
 *
 * @author Ali Elhalawati
 *
 */
public interface Entity {
	/**
	 * Returns the parsable string representation of an Entity.
	 *
	 * @return non-empty String
	 */
	String getSyntacticRepresentation();

	/**
	 * Returns the parsable string representation of an Entity while replacing variables.
	 *
	 * @return non-empty String
	 */
	default String getSyntacticRepresentation(Map<Variable, Term> map) {
		String representation = getSyntacticRepresentation();
		for (Variable variable : map.keySet()) {
			representation = representation.replaceAll(variable.getSyntacticRepresentation().replaceAll("\\?", "\\\\?"), map.get(variable).getSyntacticRepresentation());
		}
		return representation;
	}
}
