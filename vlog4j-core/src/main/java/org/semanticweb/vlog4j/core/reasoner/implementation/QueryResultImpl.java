package org.semanticweb.vlog4j.core.reasoner.implementation;

/*
 * #%L
 * VLog4j Core Components
 * %%
 * Copyright (C) 2018 VLog4j Developers
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

import org.semanticweb.vlog4j.core.model.api.QueryResult;
import org.semanticweb.vlog4j.core.model.api.Term;

/**
 * Implements {@link QueryResult}s. 
 * @author Irina Dragoste
 *
 */
public final class QueryResultImpl implements QueryResult {

	private final List<Term> terms;

	public QueryResultImpl(List<Term> terms) {
		this.terms = terms;
	}

	@Override
	public List<Term> getTerms() {
		return this.terms;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.terms == null ? 0 : this.terms.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof QueryResult)) {
			return false;
		}
		final QueryResult other = (QueryResult) obj;
		if (this.terms == null) {
			return other.getTerms() == null;
		} else {
			return this.terms.equals(other.getTerms());
		}
	}

	@Override
	public String toString() {
		return "QueryResult [terms=" + this.terms + "]";
	}

}
