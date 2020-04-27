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

import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.LogLevel;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.core.reasoner.implementation.VLogReasoner;
import org.semanticweb.rulewerk.parser.ParsingException;
import org.semanticweb.rulewerk.parser.RuleParser;

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

		// Loaded asp files (encoding and instance)

		// Transform rules 
		// - by introducing a helper predicate for each rule
		// - simplifying (remove negation, disjunction in head to conjunction)

		// Execute reasoning

		// Get grounded rules

		/* Configure rules */
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

		try (Reasoner reasoner = new VLogReasoner(kb)) {
			reasoner.setLogFile(ExamplesUtils.OUTPUT_FOLDER + "vlog.log");
			reasoner.setLogLevel(LogLevel.DEBUG);

			/* Initialise reasoner and compute inferences */
			reasoner.reason();

			/* Execute some queries */
			final List<String> queries = Arrays.asList("colour(?X)", "coloured(?X, ?C)", "vertex(?X)");
			System.out.println("\nNumber of inferred tuples for selected query atoms:");
			for (final String queryString : queries) {
				ExamplesUtils.printOutQueryAnswers(RuleParser.parsePositiveLiteral(queryString), reasoner);
				double answersCount = reasoner.countQueryAnswers(RuleParser.parsePositiveLiteral(queryString)).getCount();
				System.out.println("  " + queryString + ": " + answersCount);
			}
		}
	}

}
