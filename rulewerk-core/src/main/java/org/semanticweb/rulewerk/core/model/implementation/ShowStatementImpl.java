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

import java.util.ArrayList;
import java.util.List;

public class ShowStatementImpl implements ShowStatement {

	final Predicate predicate;

	/**
	 * Creates a show statement for the given predicate.
	 *
	 * @param predicate the predicate to show
	 */
	public ShowStatementImpl(final Predicate predicate) {
		this.predicate = predicate;
	}

	@Override
	public Predicate getPredicate() {
		return this.predicate;
	}

	@Override
	public PositiveLiteral getQueryLiteral() {
		List<Term> terms = new ArrayList<>(predicate.getArity());
		for (int i=0; i<predicate.getArity(); i++) {
			terms.add(new UniversalVariableImpl("Var" + i));
		}
		return new PositiveLiteralImpl(getPredicate(), terms);
	}

	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}
}
