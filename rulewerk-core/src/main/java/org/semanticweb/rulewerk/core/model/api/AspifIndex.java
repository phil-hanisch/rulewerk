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
 * Interface for representing the information storage that is needed for an aspif grounding
 *
 * @author Philipp Hanisch
 *
 */
public interface AspifIndex {

	/**
	 * Get the aspif integer representing a given literal
	 *
	 * @param literal a literal
	 * @param groundingMap a map containing a term for each variable
	 * @return the aspif integer
	 */
	int getAspifInteger(Literal literal, Map<Variable, Long> groundingMap);

	int getAspifInteger(Fact fact);
}
