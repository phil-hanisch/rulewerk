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

import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.LogLevel;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;
import org.semanticweb.rulewerk.core.reasoner.implementation.VLogReasoner;
import org.semanticweb.rulewerk.parser.ParsingException;
import org.semanticweb.rulewerk.parser.RuleParser;
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
 * @author 
 */
public class AspExample {

	public static void main(final String[] args) throws IOException, ParsingException {
		ExamplesUtils.configureLogging();

		/* Load rules and facts from asp file */
		KnowledgeBase kb;
		try {
			kb = RuleParser.parse(new FileInputStream(ExamplesUtils.INPUT_FOLDER + "asp/colouring-encoding.rls"));
		} catch (final ParsingException e) {
			System.out.println("Failed to parse rules: " + e.getMessage());
			return;
		}
		System.out.println("Rules used in this example:");
		kb.getRules().forEach(System.out::println);
		System.out.println("");

		System.out.println("Facts used in this example:");
		kb.getFacts().forEach(System.out::println);
		System.out.println("");

		/* Analyse rule structure */
		Set<Predicate> unsafePredicates = new HashSet<Predicate>();
		Map<Predicate, Set<Predicate>> dependencyMap = new HashMap<Predicate, Set<Predicate>>();
		for (Rule rule : kb.getRules()) {
			for (Literal literal : rule.getHead().getLiterals()) {
				Predicate pred = literal.getPredicate();
				if (rule.isApproximated()) {
					unsafePredicates.add(pred);
				}

				if (!dependencyMap.containsKey(pred)) {
					dependencyMap.put(pred, new HashSet<Predicate>());
				}

				Set<Predicate> dependencies = dependencyMap.get(pred);
				dependencies.addAll(rule.getBody().getLiterals().stream().map(Literal::getPredicate).collect(Collectors.toList()));
			}
		}

		boolean changed = unsafePredicates.size() > 0;
		while (changed) {
			changed = false;
			for (Predicate pred : dependencyMap.keySet()) {
				if (!unsafePredicates.contains(pred) && dependencyMap.get(pred).stream().anyMatch(unsafePredicates::contains)) {
					unsafePredicates.add(pred);
					changed = true;
				}
			}
		}

		System.out.println("Unsafe predicates");
		for (Predicate pred : unsafePredicates) {
			System.out.println(pred);
		}

		/* Construct modified knowledge base 
		 * - introduce a helper predicate for each rule
		 * - remove negation (currently not done)
		 * - transform disjunction in head to conjunction (currently implicitly)
		 */
		KnowledgeBase kbModified = new KnowledgeBase();
		kbModified.addStatements(kb.getFacts());
		int ruleIdx = 0;
		List<PositiveLiteral> helperLiterals = new ArrayList();
		for (Rule rule : kb.getRules()) {
			String bodyUniversalVariableNames = rule.getBody().getUniversalVariables()
								   .reduce("", (partialNames, variable) -> partialNames.equals("") 
								   		? variable.toString()
								   		: partialNames + "," + variable.toString(), String::concat);
		    String helperString = "rule" + ruleIdx + "(" + bodyUniversalVariableNames + ")";
	   		PositiveLiteral helperLiteral = RuleParser.parsePositiveLiteral(helperString);
			Conjunction helperConjunction = new ConjunctionImpl(Arrays.asList(helperLiteral));

			kbModified.addStatement(new RuleImpl(helperConjunction, rule.getBody()));
			kbModified.addStatement(new RuleImpl(rule.getHead(), helperConjunction));

			helperLiterals.add(helperLiteral);
			ruleIdx++;
		}

		System.out.println("Modified rules used in this example:");
		kbModified.getRules().forEach(System.out::println);
		System.out.println("");

		/* Execute reasoning */
		try (Reasoner reasoner = new VLogReasoner(kbModified)) {
			reasoner.setLogFile(ExamplesUtils.OUTPUT_FOLDER + "vlog.log");
			reasoner.setLogLevel(LogLevel.DEBUG);

			/* Initialise reasoner and compute inferences */
			reasoner.reason();

			/* Construct grounded knowledge base */
			FileWriter fileWriter = new FileWriter(ExamplesUtils.OUTPUT_FOLDER + "grounding_text.lp");

			ruleIdx = 0;
			System.out.println("Grounding started...");
			for (Rule rule : kb.getRules()) {
				// Get the helper literal for the current rule
				PositiveLiteral helperLiteral = helperLiterals.get(ruleIdx++);
				String template = rule.getSyntacticRepresentation().replaceAll("~", "not ") + "\n";
				int i = 1;
				Iterator<UniversalVariable> iterator = rule.getUniversalVariables().iterator();
				while (iterator.hasNext()) {
					template = template.replaceAll(iterator.next().getSyntacticRepresentation().replaceAll("\\?", "\\\\?"), "\\%" + String.valueOf(i) + "\\$s");
					i++;
				}
				System.out.println(template);

				try (final QueryResultIterator answers = reasoner.answerQuery(helperLiteral, true)) {
					// each query result represents a grounding
					while(answers.hasNext()) {
						String terms[] = answers.next().getTerms().stream().map(term -> term.getSyntacticRepresentation()).toArray(String[]::new);
						fileWriter.write(String.format(template, terms));
					}
				}
			}

			System.out.println("Grounding done...");
			fileWriter.close();
		}
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



