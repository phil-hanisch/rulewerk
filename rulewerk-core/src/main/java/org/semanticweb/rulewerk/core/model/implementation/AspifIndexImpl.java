package org.semanticweb.rulewerk.core.model.implementation;

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

import org.semanticweb.rulewerk.core.model.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation for an aspif index structure
 *
 * @author Philipp Hanisch
 */
public class AspifIndexImpl implements AspifIndex {

	private Integer literalCount;
	final private Map<String, Integer> literalMap;


	public AspifIndexImpl() {
		this.literalCount = 1;
		this.literalMap = new HashMap<>();
	}

	@Override
	public int getAspifInteger(Literal literal, Map<Variable, Term> groundingMap) {
		String aspifIdentifier = literal.getAspifIdentifier(groundingMap);
		Integer aspifInteger;
		if ((aspifInteger = this.literalMap.get(aspifIdentifier)) == null) {
			aspifInteger = this.literalCount++;
			literalMap.put(aspifIdentifier, aspifInteger);
		}
		return literal.isNegated() ? -aspifInteger : aspifInteger;
	}

	@Override
	public int getAspifInteger(Fact fact) {
		String aspifIdentifier = fact.getAspifIdentifier();
		Integer aspifInteger;
		if ((aspifInteger = this.literalMap.get(aspifIdentifier)) == null) {
			aspifInteger = this.literalCount++;
			literalMap.put(aspifIdentifier, aspifInteger);
		}
		return aspifInteger;
	}
}
