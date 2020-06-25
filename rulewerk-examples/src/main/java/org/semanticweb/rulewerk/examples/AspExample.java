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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.cli.*;
import org.semanticweb.rulewerk.core.model.api.ShowStatement;
import org.semanticweb.rulewerk.core.model.implementation.Grounder;
import org.semanticweb.rulewerk.core.model.implementation.ShowStatementImpl;
import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.LogLevel;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.parser.ParsingException;
import org.semanticweb.rulewerk.parser.RuleParser;
import org.semanticweb.rulewerk.core.model.api.AspRule;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.reasoner.vlog.VLogReasoner;

/**
 * This example grounds a given asp encoding by using VLog. The grounding can be shown either in a textual format or in
 * aspif. Alternatively, the grounding can be forwarded to clasp to compute the answer set(s).
 *
 * @author Philipp Hanisch
 */
public class AspExample {

	public static void main(final String[] args) throws IOException, ParsingException {
		System.out.println("Ready...");
		long startTimeOverall, startTimeParsing, startTimeVLog, startTimeOutput, startTimeClasp;
		long endTimeOverall, endTimeParsing, endTimeVLog, endTimeOutput, endTimeClasp;
		String[] programs;
		String inputPath, outputPath, instance, system;
		CommandLine line;
		boolean textFormat;
		BufferedWriter outputWriter;

		// Get start time
		startTimeOverall = System.nanoTime();

		// Prepare command line options
		Options options = new Options();
		options.addOption(Option.builder("t").longOpt("text").desc("Display the grounding in a human-readable format").build());
		options.addOption(Option.builder("a").longOpt("aspif").desc("Display the grounding in aspif").build());
		options.addOption(Option.builder("o").longOpt("output").desc("Set the output file").required().hasArg().numberOfArgs(1).build());
		options.addOption(Option.builder("p").longOpt("java-path").desc("Use java project path to input and output files").build());
		options.addOption(Option.builder("P").longOpt("path").desc("Set the path to input and output files").hasArg().build());
		options.addOption(Option.builder("i").longOpt("instance-name").desc("Set the instance name (matters only for identifying output later)").hasArg().build());
		options.addOption(Option.builder("s").longOpt("system-name").desc("Set the system name (matters only for identifying output later)").hasArg().build());

		// Parse command line arguments
		CommandLineParser parser = new DefaultParser();
		try {
			// parse the command line arguments
			line = parser.parse(options, args);
			programs = line.getArgs();
			textFormat = line.hasOption("t");
			inputPath = line.getOptionValue("P", line.hasOption("p") ? ExamplesUtils.INPUT_FOLDER : "") + "/";
			outputPath = line.getOptionValue("P", line.hasOption("p") ? ExamplesUtils.OUTPUT_FOLDER : "") + "/";
			String fileName = outputPath + line.getOptionValue("o");
			outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.US_ASCII));
			//outputWriter = new BufferedWriter(new FileWriter(outputPath + line.getOptionValue("o")));
			instance = line.getOptionValue("i", "unnamed_instance");
			system = line.getOptionValue("s", "Grounding");
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		startTimeParsing = System.nanoTime();
		// Load rules and facts from asp file
		ExamplesUtils.configureLogging();
		System.out.println("Load rules and facts...");
		KnowledgeBase kb;
		try {
			List<FileInputStream> inputStreamList = new ArrayList<>();
			for (String program : programs) {
				inputStreamList.add(new FileInputStream(inputPath + program));
			}
			SequenceInputStream inputStream = new SequenceInputStream(Collections.enumeration(inputStreamList));
			kb = RuleParser.parseAsp(inputStream);
		} catch (final ParsingException e) {
			System.out.println("Failed to parse rules: " + e.getMessage());
			return;
		}
		System.out.println("...done");
		endTimeParsing = System.nanoTime();

		// no show statement implies to show everything
		if (kb.getShowStatements().isEmpty()) {
			List<ShowStatement> showStatements = kb.getPredicates().stream().map(ShowStatementImpl::new).collect(Collectors.toList());
			kb.addStatements(showStatements);
		}

		// Analyse knowledge base
		Set<Predicate> approximatedPredicates = kb.analyseAspRulesForApproximatedPredicates();
		System.out.println("Approximated predicates:");
		approximatedPredicates.forEach(System.out::println);

		// Transform asp rules into standard rules
		for (AspRule rule : kb.getAspRules()) {
			kb.addStatements(rule.getApproximation(approximatedPredicates));
		}

		/* Execute reasoning */
		System.out.println("Trigger reasoning...");
		startTimeVLog = System.nanoTime();
		try (Reasoner reasoner = new VLogReasoner(kb)) {
			reasoner.setLogFile(outputPath + "vlog.log");
			reasoner.setLogLevel(LogLevel.DEBUG);

			/* Initialise reasoner and compute inferences */
			reasoner.reason();
			endTimeVLog = System.nanoTime();

			startTimeOutput = System.nanoTime();
			if (line.hasOption("t") || line.hasOption("a")) {
				// Compute only the grounding
				Grounder grounder = new Grounder(reasoner, kb, outputWriter, approximatedPredicates, textFormat);
				grounder.groundKnowledgeBase();
				outputWriter.close();
				endTimeOutput = System.nanoTime();
			} else {
				// Compute the answer sets
				Process clasp = Runtime.getRuntime().exec("clasp");
				BufferedWriter writerToClasp = new BufferedWriter(new OutputStreamWriter(clasp.getOutputStream(), StandardCharsets.US_ASCII));
				Grounder grounder = new Grounder(reasoner, kb, writerToClasp, approximatedPredicates, false);
				grounder.groundKnowledgeBase();
				writerToClasp.close();
				endTimeOutput = System.nanoTime();

				// Show the answer set
				startTimeClasp = System.nanoTime();
				System.out.println("Wait for clasp");
				try {
					clasp.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				BufferedReader readerFromClasp = new BufferedReader(new InputStreamReader(clasp.getInputStream()));
				String s;
				while ((s = readerFromClasp.readLine()) != null) {
					outputWriter.write(s);
					outputWriter.newLine();
				}
				outputWriter.close();
				clasp.destroy();
				endTimeClasp = System.nanoTime();
				System.out.println("TIMING [s] # " + instance + " # Clasp # " + ((float) (endTimeClasp - startTimeClasp) / 1000000000));
			}
		}

		endTimeOverall = System.nanoTime();
		System.out.println("TIMING [s] # " + instance + " # Parsing # " + ((float) (endTimeParsing - startTimeParsing) / 1000000000));
		System.out.println("TIMING [s] # " + instance + " # VLog # " + ((float) (endTimeVLog - startTimeVLog) / 1000000000));
		System.out.println("TIMING [s] # " + instance + " # " + system + " # " + ((float) (endTimeOutput - startTimeOutput) / 1000000000));
		System.out.println("TIMING [s] # " + instance + " # " + system + "Grounding # " + ((float) (endTimeOutput - startTimeOverall) / 1000000000));
		System.out.println("TIMING [s] # " + instance + " # " + system + "Overall # " + ((float) (endTimeOverall - startTimeOverall) / 1000000000));
	}
}




