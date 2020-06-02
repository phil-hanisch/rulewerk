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

import java.io.BufferedWriter;
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
 * This example grounds a given asp encoding in text format or aspif.
 * It is used to test and to show the grounding of basic elements of the asp core.
 *
 * @author Philipp Hanisch
 */
public class AspExample {

	public static void main(final String[] args) throws IOException, ParsingException {
		String instance = args[0];
		long startTimeOverall, startTimeVLog, startTimeOutput, endTimeOverall, endTimeVLog, endTimeOutput;
		startTimeOverall = System.nanoTime();

		boolean textFormat = false;

		ExamplesUtils.configureLogging();

		// Load rules and facts from asp file
		KnowledgeBase kb;
		try {
			kb = RuleParser.parseAsp(new FileInputStream(ExamplesUtils.INPUT_FOLDER + "asp/" + instance));
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
			kb.addStatements(rule.getApproximation(approximatedPredicates));
		}
		System.out.println("Rules used in this example:");
		kb.getRules().forEach(System.out::println);
		System.out.println("");

		/* Execute reasoning */
		startTimeVLog = System.nanoTime();
		try (Reasoner reasoner = new VLogReasoner(kb)) {
			reasoner.setLogFile(ExamplesUtils.OUTPUT_FOLDER + "vlog.log");
			reasoner.setLogLevel(LogLevel.DEBUG);

			/* Initialise reasoner and compute inferences */
			reasoner.reason();
			endTimeVLog = System.nanoTime();

			/* Construct grounded knowledge base */
			startTimeOutput = System.nanoTime();
			FileWriter fileWriter = new FileWriter(ExamplesUtils.OUTPUT_FOLDER + "grounding_text.lp");
			Grounder grounder = new Grounder(reasoner, new BufferedWriter(fileWriter), approximatedPredicates, textFormat);

			try {
//				if (textFormat) {
//					for (Fact fact : kb.getFacts()) {
//						fileWriter.write(fact.getSyntacticRepresentation() + "\n");
//					}
//				} else {
//					kb.getFacts().forEach(grounder::writeFactAspif);
//				}

				kb.getAspRules().forEach(rule -> {
					System.out.println(rule);
					rule.accept(grounder);
				});

				fileWriter.write("0\n");
			} catch (IOException e) {
				System.out.println("An error occurred.");
				e.printStackTrace();
			}

			fileWriter.close();
			endTimeOutput = System.nanoTime();
		}

		endTimeOverall = System.nanoTime();

		// System.out.println("TIMING [s] # " + instance + " # VLog # " + ((float) (endTimeVLog - startTimeVLog) / 1000000000));
		System.out.println("TIMING [s] # " + instance + " # StateOfPentecost # " + ((float) (endTimeOutput - startTimeOutput) / 1000000000));
		System.out.println("TIMING [s] # " + instance + " # StateOfPentecostOverall # " + ((float) (endTimeOverall - startTimeOverall) / 1000000000));
	}

	public static String getAspRepresentation(Entity entity) {
		return entity.getSyntacticRepresentation()
					 .replaceAll("~", "not ")
					 .replaceAll("fail\\((\\?\\w)?(, \\?\\w)*\\) ", "") + "\n";
	}
}



