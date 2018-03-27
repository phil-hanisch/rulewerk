package org.semanticweb.vlog4j.core.reasoner.implementation;

/*-
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

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.semanticweb.vlog4j.core.model.api.Atom;
import org.semanticweb.vlog4j.core.model.api.Constant;
import org.semanticweb.vlog4j.core.model.api.Predicate;
import org.semanticweb.vlog4j.core.model.implementation.Expressions;
import org.semanticweb.vlog4j.core.reasoner.DataSource;
import org.semanticweb.vlog4j.core.reasoner.exceptions.EdbIdbSeparationException;
import org.semanticweb.vlog4j.core.reasoner.exceptions.ReasonerStateException;

import karmaresearch.vlog.AlreadyStartedException;
import karmaresearch.vlog.EDBConfigurationException;
import karmaresearch.vlog.NotStartedException;

public class AddDataSourceTest {

	private static final String CSV_FILE_PATH = "src/test/data/input/unaryFacts.csv";

	@Test
	public void testAddDataSourceExistentDataForDifferentPredicates()
			throws ReasonerStateException, EdbIdbSeparationException, AlreadyStartedException,
			EDBConfigurationException, IOException, NotStartedException {
		final Predicate predicateParity1 = Expressions.makePredicate("p", 1);
		final Constant constantA = Expressions.makeConstant("a");
		final Atom factPredicateParity2 = Expressions.makeAtom("p", constantA, constantA);
		final Atom factPredicateQarity1 = Expressions.makeAtom("q", constantA);
		final DataSource dataSource = new CsvFileDataSource(new File(CSV_FILE_PATH));

		final Reasoner reasoner = new Reasoner();
		reasoner.addFacts(factPredicateParity2, factPredicateQarity1);
		reasoner.addDataSource(Expressions.makePredicate("p", 3), dataSource);
		reasoner.addDataSource(predicateParity1, dataSource);
		reasoner.load();
		reasoner.dispose();
	}

	public void testAddDataSourceBeforeLoading() throws ReasonerStateException, EdbIdbSeparationException,
			AlreadyStartedException, EDBConfigurationException, IOException, NotStartedException {
		final Predicate predicateP = Expressions.makePredicate("p", 1);
		final Predicate predicateQ = Expressions.makePredicate("q", 1);
		final DataSource dataSource = new CsvFileDataSource(new File(CSV_FILE_PATH));
		final Reasoner reasoner = new Reasoner();
		reasoner.addDataSource(predicateP, dataSource);
		reasoner.addDataSource(predicateQ, dataSource);
		reasoner.load();
		reasoner.dispose();
	}

	@Test(expected = ReasonerStateException.class)
	public void testAddDataSourceAfterLoading() throws ReasonerStateException, EdbIdbSeparationException,
			AlreadyStartedException, EDBConfigurationException, IOException, NotStartedException {
		final Predicate predicateP = Expressions.makePredicate("p", 1);
		final Predicate predicateQ = Expressions.makePredicate("q", 1);
		final DataSource dataSource = new CsvFileDataSource(new File(CSV_FILE_PATH));
		final Reasoner reasoner = new Reasoner();
		reasoner.addDataSource(predicateP, dataSource);
		reasoner.load();
		try {
			reasoner.addDataSource(predicateQ, dataSource);
		} finally {
			reasoner.dispose();
		}
	}

	@Test(expected = ReasonerStateException.class)
	public void testAddDataSourceAfterReasoning() throws ReasonerStateException, EdbIdbSeparationException,
			AlreadyStartedException, EDBConfigurationException, IOException, NotStartedException {
		final Predicate predicateP = Expressions.makePredicate("p", 1);
		final Predicate predicateQ = Expressions.makePredicate("q", 1);
		final DataSource dataSource = new CsvFileDataSource(new File(CSV_FILE_PATH));
		final Reasoner reasoner = new Reasoner();
		reasoner.addDataSource(predicateP, dataSource);
		reasoner.load();
		reasoner.reason();
		try {
			reasoner.addDataSource(predicateQ, dataSource);
		} finally {
			reasoner.dispose();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddDataSourceNoMultipleDataSourcesForPredicate() throws ReasonerStateException, IOException {
		final Predicate predicate = Expressions.makePredicate("p", 1);
		final DataSource dataSource = new CsvFileDataSource(new File(CSV_FILE_PATH));
		final Reasoner reasoner = new Reasoner();
		reasoner.addDataSource(predicate, dataSource);
		try {
			reasoner.addDataSource(predicate, dataSource);
		} finally {
			reasoner.dispose();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddDataSourceNoFactsForPredicate() throws ReasonerStateException, IOException {
		final Predicate predicate = Expressions.makePredicate("p", 1);
		final DataSource dataSource = new CsvFileDataSource(new File(CSV_FILE_PATH));
		final Atom fact = Expressions.makeAtom(Expressions.makePredicate("p", 1), Expressions.makeConstant("a"));
		final Reasoner reasoner = new Reasoner();
		reasoner.addFacts(fact);
		try {
			reasoner.addDataSource(predicate, dataSource);
		} finally {
			reasoner.dispose();
		}
	}

	@Test(expected = NullPointerException.class)
	public void testAddDataSourcePredicateNotNull() throws ReasonerStateException, IOException {
		final DataSource dataSource = new CsvFileDataSource(new File("src/test/data/unaryFacts.csv"));
		final Reasoner reasoner = new Reasoner();
		try {
			reasoner.addDataSource(null, dataSource);
		} finally {
			reasoner.dispose();
		}
	}

	@Test(expected = NullPointerException.class)
	public void testAddDataSourceNotNullDataSource() throws ReasonerStateException {
		final Predicate predicate = Expressions.makePredicate("p", 1);
		final Reasoner reasoner = new Reasoner();
		try {
			reasoner.addDataSource(predicate, null);
		} finally {
			reasoner.dispose();
		}
	}

}
