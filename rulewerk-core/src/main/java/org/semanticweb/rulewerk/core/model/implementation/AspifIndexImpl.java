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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

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
		this.literalMap = new Object2IntOpenHashMap<>();
	}

	@Override
	public int getAspifInteger(Literal literal, Map<Variable, Long> answerMap) {
		String aspifIdentifier = getAspifIdentifier(literal, answerMap);
		int aspifInteger = getAspifValue(aspifIdentifier);
		return literal.isNegated() ? -aspifInteger : aspifInteger;
	}

	@Override
	public int getAspifInteger(Fact fact) {
		String aspifIdentifier = getAspifIdentifier(fact);
		return getAspifValue(aspifIdentifier);
	}

	@Override
	public int getAspifInteger(Literal literal, Map<Variable, Long> answerMap, int context) {
		String aspifIdentifier = context + ":" + getAspifIdentifier(literal, answerMap);
		int aspifInteger = getAspifValue(aspifIdentifier);
		return literal.isNegated() ? -aspifInteger : aspifInteger;
	}

	@Override
	public int getAspifInteger(Predicate predicate, long[] termIds) {
		String aspifIdentifier = getAspifIdentifier(predicate, termIds);
		return getAspifValue(aspifIdentifier);
	}

	/**
	 * Get the aspif value for the literal specified by the given aspif identifier.
	 *
	 * @param aspifIdentifier the identifier
	 * @return the aspif value
	 */
	private int getAspifValue(String aspifIdentifier) {
		Integer aspifInteger;
		if ((aspifInteger = this.literalMap.get(aspifIdentifier)) == null) {
			aspifInteger = this.literalCount++;
			literalMap.put(aspifIdentifier, aspifInteger);
		}
		return aspifInteger;
	}

	/**
	 * Get the aspif identifier for the grounded literal.
	 * @param literal the literal
	 * @param answerMap the map describing the grounding
	 * @return the aspif identifier
	 */
	private String getAspifIdentifier(Literal literal, Map<Variable, Long> answerMap) {
		StringBuilder builder = new StringBuilder();
		builder.append(literal.getPredicate().getName());
		for (Term term : literal.getArguments()) {
			if (term.isVariable()) {
				builder.append("_").append(answerMap.get(term));
			} else {
				try {
					// TODO: Verify that the constant id is always present
					long constantId = this.reasoner.getConstantId(term.getName());
					builder.append("_").append(constantId);
				} catch (NotStartedException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Get the aspif identifier for the grounded literal, given as predicate and terms.
	 *
	 * @param predicate the predicate
	 * @param termIds the term ids
	 * @return the aspif identifier
	 */
	private String getAspifIdentifier(Predicate predicate, long[] termIds) {
		StringBuilder builder = new StringBuilder();
		builder.append(predicate.getName());
		for (long termId : termIds) {
			builder.append("_").append(termId);
		}
		return builder.toString();
	}

	/**
	 * Get the aspif identifier for the given fact.
	 *
	 * @param fact the fact
	 * @return the aspif identifier
	 */
	private String getAspifIdentifier(Fact fact) {
		StringBuilder builder = new StringBuilder();
		builder.append(fact.getPredicate().getName());
		for (Term term : fact.getArguments()) {
			if (term.isConstant()) {
				// Facts should not contain variables
				try {
					// TODO: Verify that the constant id is always present
					long constantId = this.reasoner.getConstantId(term.getName());
					builder.append("_").append(constantId);
				} catch (NotStartedException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		return builder.toString();
	};
}
