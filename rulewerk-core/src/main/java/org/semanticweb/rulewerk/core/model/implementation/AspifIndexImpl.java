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

import karmaresearch.vlog.NotStartedException;
import org.semanticweb.rulewerk.core.model.api.*;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation for an aspif index structure
 *
 * @author Philipp Hanisch
 */
public class AspifIndexImpl implements AspifIndex {

	private Integer literalCount;
	final private Map<String, Integer> literalMap;
	final private Reasoner reasoner;


	public AspifIndexImpl(Reasoner reasoner) {
		this.reasoner = reasoner;
		this.literalCount = 1;
		this.literalMap = new HashMap<>();
	}

	@Override
	public int getAspifInteger(Literal literal, Map<Variable, Long> answerMap) {
		String aspifIdentifier = getAspifIdentifier(literal, answerMap);
		Integer aspifInteger;
		if ((aspifInteger = this.literalMap.get(aspifIdentifier)) == null) {
			aspifInteger = this.literalCount++;
			literalMap.put(aspifIdentifier, aspifInteger);
		}
		return literal.isNegated() ? -aspifInteger : aspifInteger;
	}

	@Override
	public int getAspifInteger(Fact fact) {
		String aspifIdentifier = getAspifIdentifier(fact);
		Integer aspifInteger;
		if ((aspifInteger = this.literalMap.get(aspifIdentifier)) == null) {
			aspifInteger = this.literalCount++;
			literalMap.put(aspifIdentifier, aspifInteger);
		}
		return aspifInteger;
	}

	private String getAspifIdentifier(Literal literal, Map<Variable, Long> answerMap) {
		StringBuilder builder = new StringBuilder();
		builder.append(literal.getPredicate().getSyntacticRepresentation());
		for (Term term : literal.getArguments()) {
			if (term instanceof Variable) {
				builder.append("_").append(answerMap.get(term));
			} else {
				try {
					long constantId = this.reasoner.getOrAddConstantId(term.getName());
					builder.append("_").append(constantId);
				} catch (NotStartedException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		return builder.toString();
	}

	private String getAspifIdentifier(Fact fact) {
		StringBuilder builder = new StringBuilder();
		builder.append(fact.getPredicate().getSyntacticRepresentation());
		for (Term term : fact.getArguments()) {
			if (term.isConstant()) {
				// Facts should not contain variables
				try {
					long constantId = this.reasoner.getOrAddConstantId(term.getName());
					builder.append("_").append(constantId);
				} catch (NotStartedException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		return builder.toString();
	};
}
