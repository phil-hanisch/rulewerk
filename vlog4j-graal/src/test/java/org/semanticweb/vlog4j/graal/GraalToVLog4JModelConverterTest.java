package org.semanticweb.vlog4j.graal;

/*-
 * #%L
 * VLog4J Graal Import Components
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

import static org.junit.Assert.assertEquals;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeAtom;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeConjunction;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeConstant;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makePredicate;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeRule;
import static org.semanticweb.vlog4j.core.model.implementation.Expressions.makeVariable;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semanticweb.vlog4j.core.model.api.Atom;
import org.semanticweb.vlog4j.core.model.api.Constant;
import org.semanticweb.vlog4j.core.model.api.Predicate;
import org.semanticweb.vlog4j.core.model.api.Rule;
import org.semanticweb.vlog4j.core.model.api.Variable;

import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.io.ParseException;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.DefaultConjunctiveQuery;
import fr.lirmm.graphik.graal.core.DefaultRule;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;

/**
 * @author Adrian Bielefeldt
 */
public class GraalToVLog4JModelConverterTest {
	
	@org.junit.Rule
	public ExpectedException thrown = ExpectedException.none();

	private final String socrate = "socrate";
	private final String redsBike = "redsBike";

	private final String bicycle = "bicycle";
	private final String hasPart = "hasPart";
	private final String human = "human";
	private final String mortal = "mortal";
	private final String wheel = "wheel";
	
	private final String x = "X";
	private final String y = "Y";
	private final String z = "Z";
	
	private final Constant vlog4j_socrate = makeConstant(socrate);
	private final Constant vlog4j_redsBike = makeConstant(redsBike);
	
	private final Predicate vlog4j_bicycle = makePredicate(bicycle, 1);
	private final Predicate vlog4j_hasPart = makePredicate(hasPart, 2);
	private final Predicate vlog4j_human = makePredicate(human, 1);
	private final Predicate vlog4j_mortal = makePredicate(mortal, 1);
	private final Predicate vlog4j_wheel = makePredicate(wheel, 1);
	
	private final Variable vlog4j_x = makeVariable(x);
	private final Variable vlog4j_y = makeVariable(y);
	private final Variable vlog4j_z = makeVariable(z);

	private final DefaultTermFactory termFactory = new DefaultTermFactory();

	private final fr.lirmm.graphik.graal.api.core.Constant graal_socrate = termFactory.createConstant(socrate);
	private final fr.lirmm.graphik.graal.api.core.Constant graal_redsBike = termFactory.createConstant(redsBike);
	
	private final fr.lirmm.graphik.graal.api.core.Predicate graal_bicycle = new fr.lirmm.graphik.graal.api.core.Predicate(
			bicycle, 1);
	private final fr.lirmm.graphik.graal.api.core.Predicate graal_hasPart = new fr.lirmm.graphik.graal.api.core.Predicate(
			hasPart, 2);
	private final fr.lirmm.graphik.graal.api.core.Predicate graal_human = new fr.lirmm.graphik.graal.api.core.Predicate(
			human, 1);
	private final fr.lirmm.graphik.graal.api.core.Predicate graal_mortal = new fr.lirmm.graphik.graal.api.core.Predicate(
			mortal, 1);
	private final fr.lirmm.graphik.graal.api.core.Predicate graal_wheel = new fr.lirmm.graphik.graal.api.core.Predicate(
			wheel, 1);

	private final fr.lirmm.graphik.graal.api.core.Variable graal_x = termFactory.createVariable(x);
	private final fr.lirmm.graphik.graal.api.core.Variable graal_y = termFactory.createVariable(y);
	private final fr.lirmm.graphik.graal.api.core.Variable graal_z = termFactory.createVariable(z);

	@Test
	public void testConvertAtom() throws ParseException {
		final Atom vlog4j_atom = makeAtom(vlog4j_human, vlog4j_socrate);
		final fr.lirmm.graphik.graal.api.core.Atom graal_atom = new DefaultAtom(graal_human, graal_socrate);
		assertEquals(vlog4j_atom, GraalToVLog4JModelConverter.convertAtom(graal_atom));

		final Atom vlog4j_atom_2 = makeAtom(vlog4j_hasPart, vlog4j_redsBike, vlog4j_socrate);
		final fr.lirmm.graphik.graal.api.core.Atom graal_atom_2 = new DefaultAtom(graal_hasPart, graal_redsBike,
				graal_socrate);
		assertEquals(vlog4j_atom_2, GraalToVLog4JModelConverter.convertAtom(graal_atom_2));
	}

	@Test
	public void testConvertRule() throws ParseException {
		// moral(X) :- human(X)
		final Atom vlog4j_mortal_atom = makeAtom(vlog4j_mortal, vlog4j_x);
		final Atom vlog4j_human_atom = makeAtom(vlog4j_human, vlog4j_x);
		final Rule vlog4j_rule = makeRule(vlog4j_mortal_atom, vlog4j_human_atom);

		final fr.lirmm.graphik.graal.api.core.Atom graal_mortal_atom = new DefaultAtom(graal_mortal, graal_x);
		final fr.lirmm.graphik.graal.api.core.Atom graal_human_atom = new DefaultAtom(graal_human, graal_x);
		final fr.lirmm.graphik.graal.api.core.Rule graal_rule = new DefaultRule(
				new LinkedListAtomSet(graal_human_atom), new LinkedListAtomSet(graal_mortal_atom));

		assertEquals(vlog4j_rule, GraalToVLog4JModelConverter.convertRule(graal_rule));
	}
	
	@Test
	public void testConvertExistentialRule() throws ParseException {
		// hasPart(X, Y), wheel(Y) :- bicycle(X)
		final Atom vlog4j_hasPart_atom = makeAtom(vlog4j_hasPart, vlog4j_x, vlog4j_y);
		final Atom vlog4j_wheel_atom = makeAtom(vlog4j_wheel, vlog4j_y);
		final Atom vlog4j_bicycle_atom = makeAtom(vlog4j_bicycle, vlog4j_x);
		final Rule vlog4j_rule = makeRule(makeConjunction(vlog4j_hasPart_atom, vlog4j_wheel_atom), makeConjunction(vlog4j_bicycle_atom));

		final fr.lirmm.graphik.graal.api.core.Atom graal_hasPart_atom = new DefaultAtom(graal_hasPart, graal_x,
				graal_y);
		final fr.lirmm.graphik.graal.api.core.Atom graal_wheel_atom = new DefaultAtom(graal_wheel, graal_y);
		final fr.lirmm.graphik.graal.api.core.Atom graal_bicycle_atom = new DefaultAtom(graal_bicycle, graal_x);
		final fr.lirmm.graphik.graal.api.core.Rule graal_rule = new DefaultRule(
				new LinkedListAtomSet(graal_bicycle_atom), new LinkedListAtomSet(graal_hasPart_atom, graal_wheel_atom));

		assertEquals(vlog4j_rule, GraalToVLog4JModelConverter.convertRule(graal_rule));
	}
	
	@Test
	public void testConvertQuery() throws ParseException {
		// ?(X) :- mortal(X)
		final String mortalQuery = "mortalQuery";
		final Atom query = makeAtom(makePredicate(mortalQuery, 1), vlog4j_x);
		final Rule queryRule = makeRule(query, makeAtom(vlog4j_mortal, vlog4j_x));
		
		final fr.lirmm.graphik.graal.api.core.Atom graal_query_atom = new DefaultAtom(graal_mortal, graal_x);

		final ConjunctiveQuery graal_query = new DefaultConjunctiveQuery(new LinkedListAtomSet(graal_query_atom),
				Arrays.asList(graal_x));

		final GraalConjunctiveQueryToRule importedQuery = GraalToVLog4JModelConverter.convertQuery(mortalQuery,
				graal_query);
		assertEquals(query, importedQuery.getQueryAtom());
		assertEquals(queryRule, importedQuery.getRule());

		final String complexQuery = "complexQuery";
		final String predicate1 = "predicate1";
		final String predicate2 = "predicate2";
		final String predicate3 = "predicate3";
		final String predicate4 = "predicate4";
		final String stockholm = "stockholm";

		final Atom complexQueryAtom = makeAtom(makePredicate(complexQuery, 3), vlog4j_x, vlog4j_x, vlog4j_y);

		final Atom vlog4j_predicate1_atom = makeAtom(makePredicate(predicate1, 1), vlog4j_x);
		final Atom vlog4j_predicate2_atom = makeAtom(makePredicate(predicate2, 2), vlog4j_y, vlog4j_x);
		final Atom vlog4j_predicate3_atom = makeAtom(makePredicate(predicate3, 2), vlog4j_y, makeConstant(stockholm));
		final Atom vlog4j_predicate4_atom = makeAtom(makePredicate(predicate4, 3), vlog4j_x, vlog4j_y, vlog4j_z);

		final Rule complexQueryRule = makeRule(complexQueryAtom, vlog4j_predicate1_atom, vlog4j_predicate2_atom,
				vlog4j_predicate3_atom, vlog4j_predicate4_atom);

		final fr.lirmm.graphik.graal.api.core.Predicate graal_predicate1 = new fr.lirmm.graphik.graal.api.core.Predicate(
				predicate1, 1);
		final fr.lirmm.graphik.graal.api.core.Predicate graal_predicate2 = new fr.lirmm.graphik.graal.api.core.Predicate(
				predicate2, 2);
		final fr.lirmm.graphik.graal.api.core.Predicate graal_predicate3 = new fr.lirmm.graphik.graal.api.core.Predicate(
				predicate3, 2);
		final fr.lirmm.graphik.graal.api.core.Predicate graal_predicate4 = new fr.lirmm.graphik.graal.api.core.Predicate(
				predicate4, 3);

		final fr.lirmm.graphik.graal.api.core.Atom graal_predicate1_atom = new DefaultAtom(graal_predicate1, graal_x);
		final fr.lirmm.graphik.graal.api.core.Atom graal_predicate2_atom = new DefaultAtom(graal_predicate2, graal_y,
				graal_x);
		final fr.lirmm.graphik.graal.api.core.Atom graal_predicate3_atom = new DefaultAtom(graal_predicate3, graal_y,
				termFactory.createConstant(stockholm));
		final fr.lirmm.graphik.graal.api.core.Atom graal_predicate4_atom = new DefaultAtom(graal_predicate4, graal_x,
				graal_y, graal_z);

		final ConjunctiveQuery graal_complex_query = new DefaultConjunctiveQuery(
				new LinkedListAtomSet(graal_predicate1_atom, graal_predicate2_atom, graal_predicate3_atom,
						graal_predicate4_atom),
				Arrays.asList(graal_x, graal_x, graal_y));

		final GraalConjunctiveQueryToRule importedComplexQuery = GraalToVLog4JModelConverter.convertQuery(complexQuery,
				graal_complex_query);
		assertEquals(complexQueryAtom, importedComplexQuery.getQueryAtom());
		assertEquals(complexQueryRule, importedComplexQuery.getRule());
	}

	@Test
	public void testConvertQueryExceptionNoVariables() {
		thrown.expect(GraalConvertException.class);

		final fr.lirmm.graphik.graal.api.core.Atom graal_atom = new DefaultAtom(graal_hasPart, graal_x, graal_socrate);
		final ConjunctiveQuery graal_query_without_answer_variables = new DefaultConjunctiveQuery(
				new LinkedListAtomSet(graal_atom), new ArrayList<>());
		GraalToVLog4JModelConverter.convertQuery("name", graal_query_without_answer_variables);
	}

	@Test
	public void testConvertQueryExceptionEmptyBody() {
		thrown.expect(GraalConvertException.class);

		final ConjunctiveQuery graal_query_without_body = new DefaultConjunctiveQuery(new LinkedListAtomSet(),
				Arrays.asList(graal_y));
		GraalToVLog4JModelConverter.convertQuery("name", graal_query_without_body);
	}

	@Test
	public void testConvertQueryExceptionBlankPredicate() {

		thrown.expect(GraalConvertException.class);
	
		final fr.lirmm.graphik.graal.api.core.Atom graal_atom_1 = new DefaultAtom(graal_hasPart, graal_redsBike,
				graal_z);
		final fr.lirmm.graphik.graal.api.core.Atom graal_atom_2 = new DefaultAtom(graal_human, graal_z);
		final ConjunctiveQuery graal_query = new DefaultConjunctiveQuery(
				new LinkedListAtomSet(graal_atom_1, graal_atom_2), Arrays.asList(graal_z));

		GraalToVLog4JModelConverter.convertQuery(" ", graal_query);
	}
}
