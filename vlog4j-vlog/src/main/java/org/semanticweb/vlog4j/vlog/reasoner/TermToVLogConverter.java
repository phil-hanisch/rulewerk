package org.semanticweb.vlog4j.vlog.reasoner;

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

import org.semanticweb.vlog4j.core.model.api.NamedNull;
import org.semanticweb.vlog4j.core.model.api.TermType;
import org.semanticweb.vlog4j.core.model.api.AbstractConstant;
import org.semanticweb.vlog4j.core.model.api.Constant;
import org.semanticweb.vlog4j.core.model.api.DatatypeConstant;
import org.semanticweb.vlog4j.core.model.api.ExistentialVariable;
import org.semanticweb.vlog4j.core.model.api.LanguageStringConstant;
import org.semanticweb.vlog4j.core.model.api.TermVisitor;
import org.semanticweb.vlog4j.core.model.api.UniversalVariable;

/**
 * A visitor that converts {@link Term}s of different types to corresponding
 * internal VLog model {@link karmaresearch.vlog.Term}s.
 * 
 * @author Irina Dragoste
 *
 */
class TermToVLogConverter implements TermVisitor<karmaresearch.vlog.Term> {

	/**
	 * Transforms an abstract constant to a {@link karmaresearch.vlog.Term} with the
	 * same name and type {@link karmaresearch.vlog.Term.TermType#CONSTANT}.
	 */
	@Override
	public karmaresearch.vlog.Term visit(AbstractConstant term) {
		return new karmaresearch.vlog.Term(karmaresearch.vlog.Term.TermType.CONSTANT, getVLogNameForConstant(term));
	}

	/**
	 * Transforms a datatype constant to a {@link karmaresearch.vlog.Term} with the
	 * same name and type {@link karmaresearch.vlog.Term.TermType#CONSTANT}.
	 */
	@Override
	public karmaresearch.vlog.Term visit(DatatypeConstant term) {
		return new karmaresearch.vlog.Term(karmaresearch.vlog.Term.TermType.CONSTANT, term.getName());
	}

	/**
	 * Transforms a language-tagged string constant to a
	 * {@link karmaresearch.vlog.Term} with the same name and type
	 * {@link karmaresearch.vlog.Term.TermType#CONSTANT}.
	 */
	@Override
	public karmaresearch.vlog.Term visit(LanguageStringConstant term) {
		return new karmaresearch.vlog.Term(karmaresearch.vlog.Term.TermType.CONSTANT, term.getName());
	}

	/**
	 * Converts the given constant to the name of a constant in VLog.
	 * 
	 * @param constant
	 * @return VLog constant string
	 */
	public static String getVLogNameForConstant(Constant constant) {
		if (constant.getType() == TermType.ABSTRACT_CONSTANT) {
			String vLog4jConstantName = constant.getName();
			if (vLog4jConstantName.contains(":")) { // enclose IRIs with < >
				return "<" + vLog4jConstantName + ">";
			} else { // keep relative IRIs unchanged
				return vLog4jConstantName;
			}
		} else { // datatype literal
			return constant.getName();
		}
	}

	/**
	 * Converts the string representation of a constant in VLog4j directly to the
	 * name of a constant in VLog, without parsing it into a {@link Constant} first.
	 * 
	 * @param vLog4jConstantName
	 * @return VLog constant string
	 */
	public static String getVLogNameForConstantName(String vLog4jConstantName) {
		if (vLog4jConstantName.startsWith("\"")) { // keep datatype literal strings unchanged
			return vLog4jConstantName;
		} else if (vLog4jConstantName.contains(":")) { // enclose IRIs with < >
			return "<" + vLog4jConstantName + ">";
		} else { // keep relative IRIs unchanged
			return vLog4jConstantName;
		}
	}

	/**
	 * Transforms a universal variable to a {@link karmaresearch.vlog.Term} with the
	 * same name and type {@link karmaresearch.vlog.Term.TermType#VARIABLE}.
	 */
	@Override
	public karmaresearch.vlog.Term visit(UniversalVariable term) {
		return new karmaresearch.vlog.Term(karmaresearch.vlog.Term.TermType.VARIABLE, term.getName());
	}

	/**
	 * Transforms an existential variable to a {@link karmaresearch.vlog.Term} with
	 * the same name and type {@link karmaresearch.vlog.Term.TermType#VARIABLE}.
	 */
	@Override
	public karmaresearch.vlog.Term visit(ExistentialVariable term) {
		return new karmaresearch.vlog.Term(karmaresearch.vlog.Term.TermType.VARIABLE, "!" + term.getName());
	}

	/**
	 * Transforms a named null to a {@link karmaresearch.vlog.Term} with the same name
	 * and type {@link karmaresearch.vlog.Term.TermType#BLANK}.
	 */
	@Override
	public karmaresearch.vlog.Term visit(NamedNull term) {
		return new karmaresearch.vlog.Term(karmaresearch.vlog.Term.TermType.BLANK, term.getName());
	}

}
