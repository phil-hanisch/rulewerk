package org.semanticweb.rulewerk.core.model.implementation;

import java.util.Set;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.io.FileWriter;
import java.io.IOException;

import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;
import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.model.api.Conjunction;
import org.semanticweb.rulewerk.core.model.api.Literal;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.DisjunctiveRule;
import org.semanticweb.rulewerk.core.model.api.StatementVisitor;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.model.api.UniversalVariable;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;

/**
 * Implementation for {@link Disjunctive}. Represents a disjunctive asp rule.
 * 
 * @author Philipp Hanisch
 *
 */
public class DisjunctiveRuleImpl implements DisjunctiveRule {

	final Conjunction<Literal> body;
	final Conjunction<PositiveLiteral> head;
	final int ruleIdx;
	
	/**
	 * Creates a Rule with a (possibly empty) body and an non-empty head. All variables in
	 * the body must be universally quantified; all variables in the head that do
	 * not occur in the body must be existentially quantified.
	 *
	 * @param head list of positive literals (non-negated) representing the rule
	 *             head conjuncts.
	 * @param body list of Literals (negated or non-negated) representing the rule
	 *             body conjuncts.
	 */
	public DisjunctiveRuleImpl(final Conjunction<PositiveLiteral> head, final Conjunction<Literal> body, final int ruleIdx) {
		Validate.notNull(head);
		Validate.notNull(body);
		Validate.notEmpty(head.getLiterals(),
				"Empty rule head not supported. To capture integrity constraints, use a dedicated predicate that represents a contradiction.");
		if (body.getExistentialVariables().count() > 0) {
			throw new IllegalArgumentException(
					"Rule body cannot contain existential variables. Rule was: " + head + " :- " + body);
		}
		Set<UniversalVariable> bodyVariables = body.getUniversalVariables().collect(Collectors.toSet());
		// if (head.getUniversalVariables().filter(x -> !bodyVariables.contains(x)).count() > 0) {
		// 	throw new IllegalArgumentException(
		// 			"Universally quantified variables in rule head must also occur in rule body. Rule was: " + head
		// 					+ " :- " + body);
		// }

		this.head = head;
		this.body = body;
		this.ruleIdx = ruleIdx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = this.body.hashCode();
		result = prime * result + this.head.hashCode();
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
		if (!(obj instanceof DisjunctiveRule)) {
			return false;
		}
		final DisjunctiveRule other = (DisjunctiveRule) obj;

		return this.head.equals(other.getHeadLiterals()) && this.body.equals(other.getBody());
	}

	@Override
	public String toString() {
		return getSyntacticRepresentation();
	}

	@Override
	public Conjunction<PositiveLiteral> getHeadLiterals() {
		return this.head;
	}

	@Override
	public Conjunction<Literal> getBody() {
		return this.body;
	}

	@Override
	public List<Rule> getApproximation() {
		List<Rule> list = new ArrayList<Rule>();

		// add helper rule for grounding global variables
		String predicateName = "rule_" + this.ruleIdx;
		List<Term> terms = this.body.getUniversalVariables().collect(Collectors.toList());
		Predicate predicate = new PredicateImpl(predicateName, terms.size());
		PositiveLiteral literal = new PositiveLiteralImpl(predicate, terms);
		Conjunction<PositiveLiteral> conjunction = new ConjunctionImpl(Arrays.asList(literal));
		list.add(new RuleImpl(conjunction, this.body));
		list.add(new RuleImpl(this.head, new ConjunctionImpl<Literal>(conjunction)));

		return list;
	}

	@Override
	public void groundRule(Reasoner reasoner, Set<Predicate> approximatedPredicates, FileWriter writer) {
		String predicateName = "rule_" + this.ruleIdx;
		List<Term> terms = this.body.getUniversalVariables().collect(Collectors.toList());
		Predicate predicate = new PredicateImpl(predicateName, terms.size());
		PositiveLiteral literal = new PositiveLiteralImpl(predicate, terms);

		String template = this.getAspTemplate(approximatedPredicates);

		try (final QueryResultIterator answers = reasoner.answerQuery(literal, true)) {
			// each query result represents a grounding
			while(answers.hasNext()) {
				String answerTerms[] = answers.next().getTerms().stream().map(term -> term.getSyntacticRepresentation()).toArray(String[]::new);
				try {
					writer.write(String.format(template, answerTerms));					
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}
			}
		}
	};

	@Override
	public boolean requiresApproximation() {
		return this.head.getLiterals().size() > 1;
	}

	@Override
	public <T> T accept(StatementVisitor<T> statementVisitor) {
		return statementVisitor.visit(this);
	}

	@Override
	public Stream<Term> getTerms() {
		return Stream.concat(this.body.getTerms(), this.head.getTerms()).distinct();
	}

	/**
	 * Returns a template string for the given rule where every predicate is replaced by a placeholder
	 * Safe predicates in the body are removed.
	 *
	 * @param approximatedPredicates a set of predicates that might be approximated (=unsafe)
	 * @return the template for grounding the rule based on facts
	 */
	private String getAspTemplate(Set<Predicate> approximatedPredicates) {
		StringBuilder builder = new StringBuilder();

		builder.append(this.head.getSyntacticRepresentation());
		builder.append(this.getBodyTemplate(approximatedPredicates));
		builder.append(" .\n");

		// replace predicate names with placeholders
		String template = builder.toString();
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
