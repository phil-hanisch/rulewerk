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

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.semanticweb.rulewerk.core.model.implementation.Grounder;
import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.LogLevel;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;
import org.semanticweb.rulewerk.core.reasoner.implementation.VLogReasoner;
import org.semanticweb.rulewerk.parser.ParsingException;
import org.semanticweb.rulewerk.parser.RuleParser;
import org.semanticweb.rulewerk.core.model.api.AspRule;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.api.Entity;
import org.semanticweb.rulewerk.core.model.api.Fact;
import org.semanticweb.rulewerk.core.model.api.Variable;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.UniversalVariable;
import org.semanticweb.rulewerk.core.model.api.Term;
import org.semanticweb.rulewerk.core.model.api.Literal;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.Conjunction;
import org.semanticweb.rulewerk.core.model.implementation.RuleImpl;
import org.semanticweb.rulewerk.core.model.implementation.ConjunctionImpl;

/**
 * This example reasons about human diseases, based on information from the
 * Disease Ontology (DOID) and Wikidata. It illustrates how to load data from
 * different sources (RDF file, SPARQL), and reason about these inputs using
 * rules that are loaded from a file. The rules used here employ existential
 * quantifiers and stratified negation.
 *
 * @author Philipp Hanisch
 */
public class AspExample {

	public static void main(final String[] args) throws IOException, ParsingException {
		ExamplesUtils.configureLogging();

		// Load rules and facts from asp file
		KnowledgeBase kb;
		try {
			kb = RuleParser.parseAsp(new FileInputStream(ExamplesUtils.INPUT_FOLDER + "asp/crossword.rls"));
			// kb = RuleParser.parseAsp(new FileInputStream(ExamplesUtils.INPUT_FOLDER + "asp/colouring-encoding.rls"));
		} catch (final ParsingException e) {
			System.out.println("Failed to parse rules: " + e.getMessage());
			return;
		}
		System.out.println("Asp rules used in this example:");
		kb.getAspRules().forEach(System.out::println);
		System.out.println("");

		// System.out.println("Facts used in this example:");
		// kb.getFacts().forEach(System.out::println);
		// System.out.println("");

		// Analyse rule structure
		Set<Predicate> approximatedPredicates = kb.analyseAspRulesForApproximatedPredicates();

		System.out.println("Approximated predicates");
		approximatedPredicates.forEach(System.out::println);
		System.out.println("");

		// Transform asp rules into standard rules
		for (AspRule rule : kb.getAspRules()) {
			kb.addStatements(rule.getApproximation());
		}
		System.out.println("Rules used in this example:");
		kb.getRules().forEach(System.out::println);
		System.out.println("");

		/* Execute reasoning */
		try (Reasoner reasoner = new VLogReasoner(kb)) {
			reasoner.setLogFile(ExamplesUtils.OUTPUT_FOLDER + "vlog.log");
			reasoner.setLogLevel(LogLevel.DEBUG);

			/* Initialise reasoner and compute inferences */
			reasoner.reason();

			/* Construct grounded knowledge base */
			FileWriter fileWriter = new FileWriter(ExamplesUtils.OUTPUT_FOLDER + "grounding_text.lp");
			Grounder grounder = new Grounder(reasoner, fileWriter, approximatedPredicates);

			for (Fact fact : kb.getFacts()) {
				try {
					fileWriter.write(fact.getSyntacticRepresentation() + "\n");
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
			    }
			}

			for (AspRule rule : kb.getAspRules()) {
				System.out.println(rule);
				rule.accept(grounder);
			}

			fileWriter.close();
		}
	}

	/**
	 * Returns a template string for the given rule where every predicate is replaced by a placeholder
	 * Safe predicates in the body are removed.
	 *
	 * @param rule the rule for which the template should be build
	 * @param unsafePredicates a set of predicates that might be approximated (=unsafe)
	 * @return the template for grounding the rule based on facts
	 */
	public static String buildRuleTemplate(Rule rule, Set<Predicate> unsafePredicates) {
		StringBuilder builder = new StringBuilder();

		builder.append(rule.getHead().getSyntacticRepresentation());

		// build body while ignoring save predicates
		boolean first = true;
		for (Literal literal : rule.getBody()) {
			if (!unsafePredicates.contains(literal.getPredicate())) {
				continue;
			}

			if (first) {
				builder.append(" :- ");
				first = false;
			} else {
				builder.append(", ");
			}

			builder.append(literal.getSyntacticRepresentation().replace("~", "not "));
		}

		builder.append(" .\n");

		// replace predicate names with placeholders
		String template = builder.toString();
		Iterator<UniversalVariable> iterator = rule.getUniversalVariables().iterator();
		int i = 1;
		while (iterator.hasNext()) {
			template = template.replaceAll(iterator.next().getSyntacticRepresentation().replaceAll("\\?", "\\\\?"), "\\%" + String.valueOf(i) + "\\$s");
			i++;
		}
		System.out.println(template);

		return template;
	}

	public static void writeKnowledgeBaseToFile(KnowledgeBase kb, String fileName) {
		try {
			FileWriter fileWriter = new FileWriter(fileName);
			for (Fact fact : kb.getFacts()) {
				fileWriter.write(getAspRepresentation(fact));
			}
			for (Rule rule : kb.getRules()) {
				fileWriter.write(getAspRepresentation(rule));
			}
			fileWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
	    }
	}

	public static String getAspRepresentation(Entity entity) {
		return entity.getSyntacticRepresentation()
					 .replaceAll("~", "not ")
					 .replaceAll("fail\\((\\?\\w)?(, \\?\\w)*\\) ", "") + "\n";
	}
}



