package org.semanticweb.vlog4j.core.reasoner.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeConjunction;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeConstant;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makePositiveConjunction;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makePositiveLiteral;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeRule;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeVariable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.semanticweb.vlog4j.core.model.api.Conjunction;
import org.semanticweb.vlog4j.core.model.api.Constant;
import org.semanticweb.vlog4j.core.model.api.Literal;
import org.semanticweb.vlog4j.core.model.api.PositiveLiteral;
import org.semanticweb.vlog4j.core.model.api.Predicate;
import org.semanticweb.vlog4j.core.model.api.Rule;
import org.semanticweb.vlog4j.core.model.api.Variable;
import org.semanticweb.vlog4j.core.model.implementation.PredicateImpl;
import org.semanticweb.vlog4j.core.reasoner.CyclicityResult;
import org.semanticweb.vlog4j.core.reasoner.Reasoner;
import org.semanticweb.vlog4j.core.reasoner.exceptions.EdbIdbSeparationException;
import org.semanticweb.vlog4j.core.reasoner.exceptions.IncompatiblePredicateArityException;
import org.semanticweb.vlog4j.core.reasoner.exceptions.ReasonerStateException;

import karmaresearch.vlog.NotStartedException;

public class AcyclicityTest {

	final Variable x = makeVariable("x");
	final Variable y = makeVariable("y");
	final Variable z = makeVariable("z");
	final Variable w = makeVariable("w");
	final Variable v = makeVariable("v");
	final Variable u = makeVariable("u");

	final Constant a = makeConstant("a");
	final Constant b = makeConstant("b");
	final Constant c = makeConstant("c");
	final Constant d = makeConstant("d");
	final Constant e = makeConstant("e");

	final Predicate edbC = new PredicateImpl("edbC", 1);
	final Predicate idbC = new PredicateImpl("idbC", 1);
	final Predicate edbD = new PredicateImpl("edbD", 1);
	final Predicate idbD = new PredicateImpl("idbD", 1);
	final Predicate edbE = new PredicateImpl("edbE", 1);
	final Predicate idbE = new PredicateImpl("idbE", 1);

	final Predicate edbR = new PredicateImpl("edbR", 2);
	final Predicate idbR = new PredicateImpl("idbR", 2);
	final Predicate edbS = new PredicateImpl("edbS", 2);
	final Predicate idbS = new PredicateImpl("idbS", 2);
	final Predicate edbV = new PredicateImpl("edbV", 2);
	final Predicate idbV = new PredicateImpl("idbV", 2);

	final Rule mappingC = makeRule(makePositiveLiteral(idbC, x), makePositiveLiteral(edbC, x));
	final Rule mappingD = makeRule(makePositiveLiteral(idbD, x), makePositiveLiteral(edbD, x));
	final Rule mappingE = makeRule(makePositiveLiteral(idbE, x), makePositiveLiteral(edbE, x));
	final Rule mappingR = makeRule(makePositiveLiteral(idbR, x, y), makePositiveLiteral(edbR, x, y));
	final Rule mappingS = makeRule(makePositiveLiteral(idbS, x, y), makePositiveLiteral(edbS, x, y));
	final Rule mappingV = makeRule(makePositiveLiteral(idbV, x, y), makePositiveLiteral(edbV, x, y));
	final List<Rule> mappings = Arrays.asList(mappingC, mappingD, mappingE, mappingR, mappingS, mappingV);

	final PositiveLiteral instC = makePositiveLiteral(edbC, c);
	final PositiveLiteral instD = makePositiveLiteral(edbD, c);
	final PositiveLiteral instE = makePositiveLiteral(edbD, c);
	final PositiveLiteral instR = makePositiveLiteral(edbR, c, d);
	final PositiveLiteral instS = makePositiveLiteral(edbS, c, d);
	final PositiveLiteral instV = makePositiveLiteral(edbV, c, d);
	final List<PositiveLiteral> instEDBs = Arrays.asList(instC, instD, instE, instR, instS, instV);

	@Test
	public void testAcyclicityJA() throws ReasonerStateException, EdbIdbSeparationException,
			IncompatiblePredicateArityException, IOException, NotStartedException {
		// JA, RJA, MFA, RMFA, not MFC, not RMFC, and CyclicityResult.ACYCLIC

		// idbS(?y, ?z) :- idbR(?x, ?y)
		final Rule rule1 = makeRule(makePositiveLiteral(idbS, y, z), makePositiveLiteral(idbR, x, y));
		// idbV(?y, ?z) :- idbS(?x, ?y)
		final Rule rule2 = makeRule(makePositiveLiteral(idbV, y, z), makePositiveLiteral(idbS, x, y));

		try (final Reasoner reasoner = Reasoner.getInstance()) {
			reasoner.addRules(mappings);
			reasoner.addFacts(instEDBs);
			reasoner.addRules(rule1, rule2);
			reasoner.load();
			assertTrue(reasoner.isJA());
			assertTrue(reasoner.isRJA());
			assertTrue(reasoner.isMFA());
			assertTrue(reasoner.isRMFA());
			assertFalse(reasoner.isMFC());
			// Uncomment the following line when the RMFC check is implemented
			// assertFalse(reasoner.isRMFC());
			assertEquals(CyclicityResult.ACYCLIC, reasoner.checkForCycles());
		}
	}

	@Test
	public void testAcyclicityMFA() throws ReasonerStateException, EdbIdbSeparationException,
			IncompatiblePredicateArityException, IOException, NotStartedException {
		// JA, not RJA, MFA, RMFA, not MFC, not RMFC, and CyclicityResult.ACYCLIC

		// idbS(?y, ?z) :- idbC(?x), idbR(?x, ?y)
		final Rule rule1 = makeRule(makePositiveLiteral(idbS, y, z), makePositiveLiteral(idbC, x),
				makePositiveLiteral(idbR, x, y));
		// idbS(?y, ?z) :- idbC(?x), idbR(?x, ?y)
		final Rule rule2 = makeRule(makePositiveLiteral(idbR, y, z), makePositiveLiteral(idbD, x),
				makePositiveLiteral(idbS, x, y));

		try (final Reasoner reasoner = Reasoner.getInstance()) {
			reasoner.addRules(mappings);
			reasoner.addFacts(instEDBs);
			reasoner.addRules(rule1, rule2);

			reasoner.load();
			assertFalse(reasoner.isJA());
			assertFalse(reasoner.isRJA());
			assertTrue(reasoner.isMFA());
			// Uncommenting the following line results in a "fatal error"
			// assertTrue(reasoner.isRMFA());
			assertFalse(reasoner.isMFC());
			// Uncomment the following line when the RMFC check is implemented
			// assertFalse(reasoner.isRMFC());
			assertEquals(CyclicityResult.ACYCLIC, reasoner.checkForCycles());
		}
	}

	@Test
	public void testAcyclicityRJA1() throws ReasonerStateException, EdbIdbSeparationException,
			IncompatiblePredicateArityException, IOException, NotStartedException {
		// JA, RJA, not MFA, RMFA, not MFC, not RMFC, and CyclicityResult.ACYCLIC

		final Rule rule1 = makeRule(makePositiveLiteral(idbR, y, z), makePositiveLiteral(idbR, x, y));
		// idbR(?y, ?z) :- idbR(?x, ?y)
		final Rule rule2 = makeRule(makePositiveLiteral(idbR, y, x), makePositiveLiteral(idbR, x, y));
		// idbR(?y, ?x) :- idbR(?x, ?y)

		try (final Reasoner reasoner = Reasoner.getInstance()) {
			reasoner.addRules(mappings);
			reasoner.addFacts(instEDBs);
			reasoner.addRules(rule1, rule2);

			reasoner.load();
			assertFalse(reasoner.isJA());
			assertTrue(reasoner.isRJA());
			assertFalse(reasoner.isMFA());
			assertTrue(reasoner.isRMFA());
			assertFalse(reasoner.isMFC());
			// Uncomment the following line when the RMFC check is implemented
			// assertFalse(reasoner.isRMFC());
			assertEquals(CyclicityResult.ACYCLIC, reasoner.checkForCycles());
		}
	}

	@Test
	public void testAcyclicityRJA2() throws ReasonerStateException, EdbIdbSeparationException,
			IncompatiblePredicateArityException, IOException, NotStartedException {
		// JA, RJA, not MFA, RMFA, not MFC, not RMFC, and CyclicityResult.ACYCLIC

		// idbR(?x, ?y), idbC(?y) :- idbC(?x)
		Conjunction<PositiveLiteral> head1 = makePositiveConjunction(makePositiveLiteral(idbR, x, y),
				makePositiveLiteral(idbC, y));
		Conjunction<Literal> body1 = makeConjunction(makePositiveLiteral(idbC, x));
		final Rule rule1 = makeRule(head1, body1);
		// idbR(?y, ?x) :- idbR(?x, ?y)
		final Rule rule2 = makeRule(makePositiveLiteral(idbR, y, x), makePositiveLiteral(idbR, x, y));

		try (final Reasoner reasoner = Reasoner.getInstance()) {
			reasoner.addRules(mappings);
			reasoner.addFacts(instEDBs);
			reasoner.addRules(rule1, rule2);

			reasoner.load();
			assertFalse(reasoner.isJA());
			assertTrue(reasoner.isRJA());
			assertFalse(reasoner.isMFA());
			assertTrue(reasoner.isRMFA());
			assertFalse(reasoner.isMFC());
			// Uncomment the following line when the RMFC check is implemented
			// assertFalse(reasoner.isRMFC());
			assertEquals(CyclicityResult.ACYCLIC, reasoner.checkForCycles());
		}
	}

	@Test
	public void testAcyclicityRMFA() throws ReasonerStateException, EdbIdbSeparationException,
			IncompatiblePredicateArityException, IOException, NotStartedException {
		// not JA, not RJA, not MFA, RMFA, MFC, not RMFC, and
		// CyclicityResult.ACYCLIC

		// idbR(?x, ?y), idbD(?y) :- idbC(?x)
		Conjunction<PositiveLiteral> head1 = makePositiveConjunction(makePositiveLiteral(idbR, x, y),
				makePositiveLiteral(idbD, y));
		Conjunction<Literal> body1 = makeConjunction(makePositiveLiteral(idbC, x));
		final Rule rule1 = makeRule(head1, body1);
		// idbS(?x, ?y), idbE(?y) :- idbD(?x)
		Conjunction<PositiveLiteral> head2 = makePositiveConjunction(makePositiveLiteral(idbS, x, y),
				makePositiveLiteral(idbE, y));
		Conjunction<Literal> body2 = makeConjunction(makePositiveLiteral(idbD, x));
		final Rule rule2 = makeRule(head2, body2);
		// idbV(?x, ?y), idbC(?y) :- idbD(?x)
		Conjunction<PositiveLiteral> head3 = makePositiveConjunction(makePositiveLiteral(idbV, x, y),
				makePositiveLiteral(idbC, y));
		Conjunction<Literal> body3 = makeConjunction(makePositiveLiteral(idbE, x));
		final Rule rule3 = makeRule(head3, body3);
		// idbR(?z, ?x) :- idbS(?x, ?y), idbV(?y, ?z)
		Conjunction<PositiveLiteral> head4 = makePositiveConjunction(makePositiveLiteral(idbR, z, x));
		Conjunction<Literal> body4 = makeConjunction(makePositiveLiteral(idbS, x, y), makePositiveLiteral(idbV, y, z));
		final Rule rule4 = makeRule(head4, body4);

		try (final Reasoner reasoner = Reasoner.getInstance()) {
			reasoner.addRules(mappings);
			reasoner.addFacts(instEDBs);
			reasoner.addRules(rule1, rule2, rule3, rule4);

			reasoner.load();
			assertFalse(reasoner.isJA());
			assertFalse(reasoner.isRJA());
			assertFalse(reasoner.isMFA());
			assertTrue(reasoner.isRMFA());
			assertFalse(reasoner.isMFC());
			// Uncomment the following line when the RMFC check is implemented
			// assertFalse(reasoner.isRMFC());
			assertEquals(CyclicityResult.ACYCLIC, reasoner.checkForCycles());
		}
	}

	@Test
	public void testAcyclicityMFC() throws ReasonerStateException, EdbIdbSeparationException,
			IncompatiblePredicateArityException, IOException, NotStartedException {
		// not JA, not RJA, not MFA, not RMFA, MFC, RMFC, and
		// CyclicityResult.CYCLIC

		// idbR(?x, ?y), idbD(?y) :- idbC(?x)
		Conjunction<PositiveLiteral> head1 = makePositiveConjunction(makePositiveLiteral(idbR, x, y),
				makePositiveLiteral(idbD, y));
		Conjunction<Literal> body1 = makeConjunction(makePositiveLiteral(idbC, x));
		final Rule rule1 = makeRule(head1, body1);
		// idbS(?x, ?y), idbE(?y) :- idbD(?x)
		Conjunction<PositiveLiteral> head2 = makePositiveConjunction(makePositiveLiteral(idbS, x, y),
				makePositiveLiteral(idbE, y));
		Conjunction<Literal> body2 = makeConjunction(makePositiveLiteral(idbD, x));
		final Rule rule2 = makeRule(head2, body2);
		// idbV(?x, ?y), idbC(?y) :- idbD(?x)
		Conjunction<PositiveLiteral> head3 = makePositiveConjunction(makePositiveLiteral(idbV, x, y),
				makePositiveLiteral(idbC, y));
		Conjunction<Literal> body3 = makeConjunction(makePositiveLiteral(idbE, x));
		final Rule rule3 = makeRule(head3, body3);

		try (final Reasoner reasoner = Reasoner.getInstance()) {
			reasoner.addRules(mappings);
			reasoner.addFacts(instEDBs);
			reasoner.addRules(rule1, rule2, rule3);

			reasoner.load();
			assertFalse(reasoner.isJA());
			assertFalse(reasoner.isRJA());
			assertFalse(reasoner.isMFA());
			assertFalse(reasoner.isRMFA());
			assertTrue(reasoner.isMFC());
			// Uncomment the following line when the RMFC check is implemented
			// assertTrue(reasoner.isRMFC());
			assertEquals(CyclicityResult.ACYCLIC, reasoner.checkForCycles());
		}
	}
}
