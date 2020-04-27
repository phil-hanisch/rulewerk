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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.LogLevel;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;
import org.semanticweb.rulewerk.core.reasoner.implementation.VLogReasoner;
import org.semanticweb.rulewerk.parser.ParsingException;
import org.semanticweb.rulewerk.parser.RuleParser;
import org.semanticweb.rulewerk.core.model.api.Rule;
import org.semanticweb.rulewerk.core.model.api.Variable;
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

			/* Get facts for all helper predicates */
			for (int i=0; i<ruleIdx; i++) {
				PositiveLiteral helperLiteral = helperLiterals.get(i);
				System.out.println("Answers to query " + helperLiteral + " :");

				try (final QueryResultIterator answers = reasoner.answerQuery(helperLiteral, true)) {
					answers.forEachRemaining(answer -> System.out.println(" - " + answer));
					System.out.println("Query answers are: " + answers.getCorrectness());
				}
			}

			// /* Execute some queries */
			// final List<String> queries = Arrays.asList("colour(?X)", "coloured(?X, ?C)", "vertex(?X)");
			// System.out.println("\nNumber of inferred tuples for selected query atoms:");
			// for (final String queryString : queries) {
			// 	ExamplesUtils.printOutQueryAnswers(RuleParser.parsePositiveLiteral(queryString), reasoner);
			// 	double answersCount = reasoner.countQueryAnswers(RuleParser.parsePositiveLiteral(queryString)).getCount();
			// 	System.out.println("  " + queryString + ": " + answersCount);
			// }
		}

		// Get grounded rules
	}

}
