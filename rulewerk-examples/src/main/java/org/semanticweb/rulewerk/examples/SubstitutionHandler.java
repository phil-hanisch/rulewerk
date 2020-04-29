
package org.semanticweb.rulewerk.examples;

/*-
 * #%L
 * Rulewerk Examples
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
import java.util.List;
import java.util.stream.Collectors;

import org.semanticweb.rulewerk.core.model.api.TermVisitor;
import org.semanticweb.rulewerk.core.model.api.StatementVisitor;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.model.api.Variable;
import org.semanticweb.rulewerk.core.model.api.Conjunction;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.Literal;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.implementation.RuleImpl;
import org.semanticweb.rulewerk.core.model.implementation.ConjunctionImpl;
import org.semanticweb.rulewerk.core.model.implementation.PositiveLiteralImpl;
import org.semanticweb.rulewerk.core.model.implementation.NegativeLiteralImpl;

public class SubstitutionHandler {

	final Map<Variable, Term> substitutionMap;

	public SubstitutionHandler(Map<Variable, Term> substitutionMap) {
		this.substitutionMap = substitutionMap; 
	}

	public Rule substituteRule(Rule rule) {
		Conjunction<PositiveLiteral> head = substituteHead(rule.getHead());
		Conjunction<Literal> body = substituteBody(rule.getBody());
		return new RuleImpl(head, body);
	}

	public Conjunction<Literal> substituteBody(Conjunction<Literal> conjunction) {
		List<Literal> literals = conjunction.getLiterals()
									  .stream()
									  .map(literal -> substituteLiteral(literal))
									  .collect(Collectors.toList());
		return new ConjunctionImpl(literals);
	}

	public Conjunction<PositiveLiteral> substituteHead(Conjunction<PositiveLiteral> conjunction) {
		List<PositiveLiteral> literals = conjunction.getLiterals()
									  .stream()
									  .map(literal -> substitutePositiveLiteral(literal))
									  .collect(Collectors.toList());
		return new ConjunctionImpl(literals);
	}

	public PositiveLiteral substitutePositiveLiteral(PositiveLiteral literal) {
		return new PositiveLiteralImpl(literal.getPredicate(), substituteTerms(literal.getArguments()));
	}

	public Literal substituteLiteral(Literal literal) {
		return literal.isNegated()
			? new NegativeLiteralImpl(literal.getPredicate(), substituteTerms(literal.getArguments()))
			: new PositiveLiteralImpl(literal.getPredicate(), substituteTerms(literal.getArguments()));
	}

	public List<Term> substituteTerms(List<Term> terms) {
		return terms.stream()
					.map(term -> substituteTerm(term))
					.collect(Collectors.toList());
	}

	public Term substituteTerm(Term term) {
		if (term.isVariable() && this.substitutionMap.containsKey(term)) {
			return this.substitutionMap.get(term);
		} else {
			return term;
		}
	}
}
