package org.semanticweb.rulewerk.core.model.implementation;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.model.api.PrefixDeclarationRegistry;

/**
 * Implementation of {@link PrefixDeclarationRegistry} that is suitable for
 * incrementally parsing from multiple sources. When trying to merge in
 * conflicting prefix declarations, a fresh non-conflicting prefix is generated
 * instead.
 *
 * @author Maximilian Marx
 */
final public class MergingPrefixDeclarationRegistry extends AbstractPrefixDeclarationRegistry {
	/**
	 * Next index to use for generated prefix names.
	 */
	private Integer nextIndex = 0;

	/**
	 * Template string to use for generated prefix name
	 */
	private static final String GENERATED_PREFIX_TEMPLATE = "rulewerk_generated_%d"
			+ PrefixDeclarationRegistry.PREFIX_NAME_SEPARATOR;

	public MergingPrefixDeclarationRegistry() {
		super();
	}

	public MergingPrefixDeclarationRegistry(final PrefixDeclarationRegistry prefixDeclarations) {
		super();
		mergePrefixDeclarations(prefixDeclarations);
	}

	/**
	 * Sets the base namespace to the given value. If a base Iri has already been
	 * set, one of them will be added as a prefix declaration with a fresh
	 * prefixName.
	 *
	 * @param baseIri the new base namespace.
	 */
	@Override
	public void setBaseIri(String baseIri) {
		Validate.notNull(baseIri, "baseIri must not be null");
		if (baseIri == this.baseIri) {
			return;
		}

		if (this.baseIri == null) {
			this.baseIri = baseIri;
		} else if (this.baseIri == PrefixDeclarationRegistry.EMPTY_BASE) {
			// we need to keep the empty base, so that we don't
			// accidentally relativise absolute Iris to
			// baseIri. Hence, introduce baseIri as a fresh prefix.
			prefixes.put(getFreshPrefix(), baseIri);
		} else {
			prefixes.put(getFreshPrefix(), this.baseIri);
			this.baseIri = baseIri;
		}
	}

	/**
	 * Registers a prefix declaration. If prefixName is already registered, a
	 * freshly generated name will be used instead.
	 *
	 * @param prefixName the name of the prefix.
	 * @param prefixIri  the IRI of the prefix.
	 */
	@Override
	public void setPrefixIri(String prefixName, String prefixIri) {
		String name = prefixes.containsKey(prefixName) ? getFreshPrefix() : prefixName;
		prefixes.put(name, prefixIri);
	}

	/**
	 * Turn an absolute Iri into a (possibly) prefixed name. Dual to
	 * {@link AbstractPrefixDeclarationRegistry#resolvePrefixedName}.
	 *
	 * @param iri an absolute Iri to abbreviate.
	 *
	 * @return an abbreviated form of {@code iri} if an appropriate prefix is known,
	 *         or {@code iri}.
	 */
	public String unresolveAbsoluteIri(String iri) {
		Map<String, Integer> matches = new HashMap<>();
		String baseIri = getBaseIri();

		if (baseIri != PrefixDeclarationRegistry.EMPTY_BASE && iri.startsWith(baseIri) && !iri.equals(baseIri)) {
			matches.put(iri.replaceFirst(baseIri, PrefixDeclarationRegistry.EMPTY_BASE), baseIri.length());
		}

		prefixes.forEach((prefixName, prefixIri) -> {
			// only select proper prefixes here, since `eg:` is not a valid prefixed name.
			if (iri.startsWith(prefixIri) && !iri.equals(prefixIri)) {
				matches.put(iri.replaceFirst(prefixIri, prefixName), prefixIri.length());
			}
		});

		List<String> matchesByLength = new ArrayList<>(matches.keySet());
		// reverse order, so we get the longest match first
		matchesByLength.sort(Comparator.comparing(matches::get).reversed());

		if (matchesByLength.size() > 0) {
			return matchesByLength.get(0);
		} else {
			// no matching prefix
			return iri;
		}
	}

	/**
	 * Merge another set of prefix declarations.
	 *
	 * @param other the set of prefix declarations to merge. Conflicting prefixes
	 *              from {@code other} will be renamed.
	 */
	public void mergePrefixDeclarations(final PrefixDeclarationRegistry other) {
		this.setBaseIri(other.getBaseIri());

		for (Entry<String, String> prefix : other) {
			this.setPrefixIri(prefix.getKey(), prefix.getValue());
		}
	}

	private String getNextFreshPrefixCandidate() {
		return String.format(GENERATED_PREFIX_TEMPLATE, this.nextIndex++);
	}

	private String getFreshPrefix() {
		while (true) {
			String candidate = getNextFreshPrefixCandidate();

			if (!prefixes.containsKey(candidate)) {
				return candidate;
			}
		}
	}
}
