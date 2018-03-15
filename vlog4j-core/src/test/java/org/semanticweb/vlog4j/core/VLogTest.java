package org.semanticweb.vlog4j.core;

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

import java.io.IOException;

import org.junit.Ignore;
import org.semanticweb.vlog4j.core.model.validation.AtomValidationException;
import org.semanticweb.vlog4j.core.model.validation.RuleValidationException;

import junit.framework.TestCase;
import karmaresearch.vlog.AlreadyStartedException;
import karmaresearch.vlog.EDBConfigurationException;
import karmaresearch.vlog.NotStartedException;
import karmaresearch.vlog.StringQueryResultEnumeration;
import karmaresearch.vlog.VLog;
import karmaresearch.vlog.VLog.RuleRewriteStrategy;

@Ignore
public class VLogTest extends TestCase {

	public void testVLogSimpleInference()
			throws AlreadyStartedException, EDBConfigurationException, IOException, AtomValidationException, RuleValidationException, NotStartedException {

		// Creating rules and facts
		final String[][] argsAMatrix = { { "a" }, { "b" } };
		final karmaresearch.vlog.Term[] argX = { new karmaresearch.vlog.Term(karmaresearch.vlog.Term.TermType.VARIABLE, "X") };
		final karmaresearch.vlog.Atom atomBx = new karmaresearch.vlog.Atom("B", argX);
		final karmaresearch.vlog.Atom atomAx = new karmaresearch.vlog.Atom("A", argX);
		final karmaresearch.vlog.Atom[] headAtoms = { atomBx };
		final karmaresearch.vlog.Atom[] bodyAtoms = { atomAx };
		final karmaresearch.vlog.Rule[] rules = { new karmaresearch.vlog.Rule(headAtoms, bodyAtoms) };

		// Start VLog
		final VLog vlog = new VLog();
		vlog.start("", false);
		vlog.addData("A", argsAMatrix);
		vlog.setRules(rules, RuleRewriteStrategy.NONE);
		vlog.materialize(true);

		// Querying
		System.out.println("Querying atom: " + atomBx.getPredicate());
		final StringQueryResultEnumeration answers = vlog.query(atomBx);
		while (answers.hasMoreElements()) {
			final String[] answer = answers.nextElement();
			System.out.println(answer[0]);
		}
		// TODO assert

		vlog.stop();
	}
}