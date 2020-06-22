package org.semanticweb.rulewerk.core.model.api;

import org.semanticweb.rulewerk.core.model.implementation.Serializer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*-
 * #%L
 * Rulewerk Core Components
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

/**
 * Interface for classes representing an asp rule. This implementation assumes that
 * rules are defined by their head and body literals, without explicitly
 * specifying quantifiers. All variables in the body are considered universally
 * quantified; all variables in the head that do not occur in the body are
 * considered existentially quantified.
 *
 * @author Philipp Hanisch
 *
 */
public interface ChoiceRule extends AspRule {

	/**
	 * Returns a list of choice elements that occur in the head of the rule
	 */
	List<ChoiceElement> getChoiceElements();

	@Override
	default boolean requiresApproximation() {
		return true;
	};

	@Override
	default String getSyntacticRepresentation() {
		return Serializer.getString(this);
	}

	/**
	 * Get the upper bound for the choice elements
	 *
	 * @return the upper bound
	 */
	Integer getUpperBound();

	/**
	 * Get the lower bound for the choice elements
	 *
	 * @return the lower bound
	 */
	Integer getLowerBound();

	/**
	 * Returns whether the upper bound implies any restrictions
	 *
	 * @return boolean
	 */
	Boolean hasUpperBound();

	/**
	 * Returns whether the lower bound implies any restrictions
	 *
	 * @return boolean
	 */
	Boolean hasLowerBound();

	/**
	 * Returns a stream containing all global variables that appear only in the rule body.
	 *
	 * @return stream of variables
	 */
	default Stream<UniversalVariable> getBodyOnlyGlobalVariables() {
		List<Term> headGlobalVariables = this.getChoiceElements().stream().flatMap(SyntaxObject::getUniversalVariables).collect(Collectors.toList());
		return getBody().getUniversalVariables().filter(var -> !headGlobalVariables.contains(var));
	}

	/**
	 * Returns a stream containing all global variables that appear in the rule head, too.
	 *
	 * @return stream of variables
	 */
	default Stream<UniversalVariable> getRelevantGlobalVariables() {
		List<Term> headGlobalVariables = this.getChoiceElements().stream().flatMap(SyntaxObject::getUniversalVariables).collect(Collectors.toList());
		return getBody().getUniversalVariables().filter(headGlobalVariables::contains);
	}

	/**
	 * Returns a stream containing all global variables, i.e. variables that occur in the rule body. The stream is
	 * constructed in such a way that the variables that appear also in the rule head are first.
	 *
	 * @return stream of variables
	 */
	default Stream<UniversalVariable> getGlobalVariables() {
		return Stream.concat(getRelevantGlobalVariables(), getBodyOnlyGlobalVariables());
	}


}
