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

import java.util.Collections;
import java.util.List;

import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.model.api.Literal;
import org.semanticweb.rulewerk.core.model.api.Conjunction;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.model.api.ChoiceElement;

/**
 * ...
 *
 * @author Philipp Hanisch
 */
public class ChoiceElementImpl implements ChoiceElement {

	private final PositiveLiteral literal;
	private final Conjunction<Literal> context;

	/**
	 * Creates a {@link ChoiceElement} of the form "{@code PositiveLiteral} : {@code Conjunction<Literal>}".
	 *
	 * @param literal a positive literal
	 * @param context a (possibly empty) conjunction of literals
	 */
	public ChoiceElementImpl(final PositiveLiteral literal, final Conjunction<Literal> context) {
		Validate.notNull(literal, "Literal cannot be null.");
		Validate.noNullElements(context,
				"Null literals cannot appear in context. The list contains a null at position [%d].");

		this.literal = literal;
		this.context = context;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.getLiteral().hashCode();
		result = prime * result + this.getContext().hashCode();
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ChoiceElement)) {
			return false;
		}
		final ChoiceElement other = (ChoiceElement) obj;

		return this.getLiteral().equals(other.getLiteral()) && this.getContext().equals(other.getContext());
	}

	@Override
	public String toString() {
		return getSyntacticRepresentation();
	}

	@Override
	public PositiveLiteral getLiteral() {
		return this.literal;
	}

	@Override
	public Conjunction<Literal> getContext() {
		return this.context;
	}

	@Override
	public Stream<Term> getTerms() {
		return Stream.concat(this.literal.getTerms(), this.context.getTerms()).distinct();
	}

}
