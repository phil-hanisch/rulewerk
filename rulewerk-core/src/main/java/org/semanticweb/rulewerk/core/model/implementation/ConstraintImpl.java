package org.semanticweb.rulewerk.core.model.implementation;

import java.util.*;
import java.util.stream.Collectors;

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

import java.util.stream.Stream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.model.api.Conjunction;
import org.semanticweb.rulewerk.core.model.api.Literal;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.Constraint;
import org.semanticweb.rulewerk.core.model.api.StatementVisitor;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.model.api.UniversalVariable;
import org.semanticweb.rulewerk.core.model.api.QueryResult;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;

/**
 * Implementation for {@link Constraint}. Represents an asp constraint.
 *
 * @author Philipp Hanisch
 *
 */
public class ConstraintImpl implements Constraint {

	final Conjunction<Literal> body;
	final int ruleIdx;

	/**
	 * Creates a Rule with an empty head and an non-empty body. All variables in
	 * the body must be universally quantified.
	 *
	 * @param body list of Literals (negated or non-negated) representing the rule
	 *             body conjuncts.
	 * @param ruleIdx the rule index to uniquely identify the rule
	 */
	public ConstraintImpl(final Conjunction<Literal> body, final int ruleIdx) {
		Validate.notNull(body);
		Validate.notEmpty(body.getLiterals(),
				"Empty rule body for constraints not supported");
		if (body.getExistentialVariables().count() > 0) {
			throw new IllegalArgumentException(
					"Rule body cannot contain existential variables. Rule was: :- " + body);
		}
		Set<UniversalVariable> bodyVariables = body.getUniversalVariables().collect(Collectors.toSet());

		this.body = body;
		this.ruleIdx = ruleIdx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		return this.body.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Constraint)) {
			return false;
		}
		final Constraint other = (Constraint) obj;

		return this.body.equals(other.getBody());
	}

	@Override
	public String toString() {
		return getSyntacticRepresentation();
	}

	@Override
	public Conjunction<PositiveLiteral> getHeadLiterals() {
		return new ConjunctionImpl<>(new ArrayList<>());
	}

	@Override
	public Conjunction<Literal> getBody() {
		return this.body;
	}

	@Override
	public int getRuleIdx() {
		return this.ruleIdx;
	}

	@Override
	public List<Rule> getApproximation() {
		PositiveLiteral literal = getHelperLiteral();
		Conjunction<PositiveLiteral> conjunction = new ConjunctionImpl<>(Collections.singletonList(literal));
		return Collections.singletonList(new RuleImpl(conjunction, this.body));
	}

	@Override
	public void groundRule(Reasoner reasoner, Set<Predicate> approximatedPredicates, FileWriter writer) {
		PositiveLiteral literal = getHelperLiteral();
		String template = this.getAspTemplate(approximatedPredicates);

		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
			// each query result represents a grounding
			while(answers.hasNext()) {
				String[] answerTerms = answers.next().getTerms().stream().map(Term::getSyntacticRepresentation).toArray(String[]::new);
				try {
					writer.write(String.format(template, (Object[]) answerTerms));
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}
			}
		}
	};

	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}

	@Override
	public Stream<Term> getTerms() {
		return this.body.getTerms();
	}

	/**
	 * Returns a template string for the given rule where every predicate is replaced by a placeholder
	 * Safe predicates in the body are removed.
	 *
	 * @param approximatedPredicates a set of predicates that might be approximated (=unsafe)
	 * @return the template for grounding the rule based on facts
	 */
	private String getAspTemplate(Set<Predicate> approximatedPredicates) {

		// replace predicate names with placeholders
		String template = this.getBodyTemplate(approximatedPredicates) + " .\n";
		Iterator<UniversalVariable> iterator = this.getUniversalVariables().iterator();
		int i = 1;
		while (iterator.hasNext()) {
			template = template.replaceAll(iterator.next().getSyntacticRepresentation().replaceAll("\\?", "\\\\?"), "\\%" + String.valueOf(i) + "\\$s");
			i++;
		}
		System.out.println(template);

		return template;
	}
}
