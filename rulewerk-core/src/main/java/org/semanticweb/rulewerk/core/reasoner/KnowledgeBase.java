package org.semanticweb.rulewerk.core.reasoner;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.semanticweb.rulewerk.core.exceptions.PrefixDeclarationException;
import org.semanticweb.rulewerk.core.exceptions.RulewerkException;
import org.semanticweb.rulewerk.core.model.api.*;
import org.semanticweb.rulewerk.core.model.implementation.MergingPrefixDeclarationRegistry;
import org.semanticweb.rulewerk.core.model.implementation.Serializer;

/**
 * A knowledge base with rules, facts, and declarations for loading data from
 * further sources. This is a "syntactic" object in that it represents some
 * information that is not relevant for the semantics of reasoning, but that is
 * needed to ensure faithful re-serialisation of knowledge bases loaded from
 * files (e.g., preserving order).
 *
 * @author Markus Kroetzsch
 *
 */
public class KnowledgeBase implements Iterable<Statement> {

	private final Set<KnowledgeBaseListener> listeners = new HashSet<>();

	/**
	 * All (canonical) file paths imported so far, used to prevent cyclic imports.
	 */
	private final Set<String> importedFilePaths = new HashSet<>();

	/**
	 * Auxiliary class to process {@link Statement}s when added to the knowledge
	 * base. Returns true if a statement was added successfully.
	 *
	 * @author Markus Kroetzsch
	 *
	 */
	private class AddStatementVisitor implements StatementVisitor<Boolean> {
		@Override
		public Boolean visit(final Fact statement) {
			KnowledgeBase.this.addFact(statement);
			return true;
		}

		@Override
		public Boolean visit(final Rule statement) {
			return true;
		}

		@Override
		public Boolean visit(final AspRule statement) {
			return true;
		}

		@Override
		public Boolean visit(final ShowStatement statement) {
			KnowledgeBase.this.showStatements.add(statement);
			return true;
		}

		@Override
		public Boolean visit(final DataSourceDeclaration statement) {
			KnowledgeBase.this.dataSourceDeclarations.add(statement);
			return true;
		}
	}

	private final AddStatementVisitor addStatementVisitor = new AddStatementVisitor();

	/**
	 * Auxiliary class to process {@link Statement}s when removed from the knowledge
	 * base. Returns true if a statement was removed successfully.
	 *
	 * @author Irina Dragoste
	 *
	 */
	private class RemoveStatementVisitor implements StatementVisitor<Boolean> {

		@Override
		public Boolean visit(final Fact statement) {
			KnowledgeBase.this.removeFact(statement);
			return true;
		}

		@Override
		public Boolean visit(final Rule statement) {
			return true;
		}

		@Override
		public Boolean visit(final AspRule statement) {
			return true;
		}

		@Override
		public Boolean visit(ShowStatement statement) {
			KnowledgeBase.this.showStatements.remove(statement);
			return true;
		}

		@Override
		public Boolean visit(final DataSourceDeclaration statement) {
			KnowledgeBase.this.dataSourceDeclarations.remove(statement);
			return true;
		}
	}

	private final RemoveStatementVisitor removeStatementVisitor = new RemoveStatementVisitor();

	private class ExtractStatementsVisitor<T> implements StatementVisitor<Void> {

		final ArrayList<T> extracted = new ArrayList<>();
		final Class<T> ownType;

		ExtractStatementsVisitor(final Class<T> type) {
			this.ownType = type;
		}

		ArrayList<T> getExtractedStatements() {
			return this.extracted;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Void visit(final Fact statement) {
			if (this.ownType.equals(Fact.class)) {
				this.extracted.add((T) statement);
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Void visit(final Rule statement) {
			if (this.ownType.equals(Rule.class)) {
				this.extracted.add((T) statement);
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Void visit(final AspRule statement) {
			if (this.ownType.equals(AspRule.class)) {
				this.extracted.add((T) statement);
			}
			return null;
		}

		@Override
		public Void visit(final ShowStatement statement) {
			if (this.ownType.equals(ShowStatement.class)) {
				this.extracted.add((T) statement);
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Void visit(final DataSourceDeclaration statement) {
			if (this.ownType.equals(DataSourceDeclaration.class)) {
				this.extracted.add((T) statement);
			}
			return null;
		}
	}

	/**
	 * The primary storage for the contents of the knowledge base.
	 */
	private final LinkedHashSet<Statement> statements = new LinkedHashSet<>();

	/**
	 * Known prefixes that can be used to pretty-print the contents of the knowledge
	 * base. We try to preserve user-provided prefixes found in files when loading
	 * data.
	 */
	private MergingPrefixDeclarationRegistry prefixDeclarationRegistry = new MergingPrefixDeclarationRegistry();

	/**
	 * Index structure that organises all facts by their predicate.
	 */
	private final Map<Predicate, Set<PositiveLiteral>> factsByPredicate = new HashMap<>();

	/**
	 * Index structure that holds all data source declarations of this knowledge
	 * base.
	 */
	private final Set<DataSourceDeclaration> dataSourceDeclarations = new HashSet<>();

	/**
	 * Index structure that holds all show statements of this knowledge base.
	 */
	private final Set<ShowStatement> showStatements = new HashSet<>();

	/**
	 * Registers a listener for changes on the knowledge base
	 *
	 * @param listener a KnowledgeBaseListener
	 */
	public void addListener(final KnowledgeBaseListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Unregisters given listener from changes on the knowledge base
	 *
	 * @param listener KnowledgeBaseListener
	 */
	public void deleteListener(final KnowledgeBaseListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * Adds a single statement to the knowledge base.
	 *
	 * @param statement the statement to be added
	 */
	public void addStatement(final Statement statement) {
		if (this.doAddStatement(statement)) {
			this.notifyListenersOnStatementAdded(statement);
		}
	}

	/**
	 * Adds a single statement to the knowledge base.
	 *
	 * @param statement the statement to be added
	 * @return true, if the knowledge base has changed.
	 */
	boolean doAddStatement(final Statement statement) {
		Validate.notNull(statement, "Statement cannot be Null!");
		if (!this.statements.contains(statement) && statement.accept(this.addStatementVisitor)) {
			this.statements.add(statement);
			return true;
		}
		return false;
	}

	/**
	 * Adds a collection of statements to the knowledge base.
	 *
	 * @param statements the statements to be added
	 */
	public void addStatements(final Collection<? extends Statement> statements) {
		final List<Statement> addedStatements = new ArrayList<>();

		for (final Statement statement : statements) {
			if (this.doAddStatement(statement)) {
				addedStatements.add(statement);
			}
		}

		this.notifyListenersOnStatementsAdded(addedStatements);
	}

	/**
	 * Adds a list of statements to the knowledge base.
	 *
	 * @param statements the statements to be added
	 */
	public void addStatements(final Statement... statements) {
		final List<Statement> addedStatements = new ArrayList<>();

		for (final Statement statement : statements) {
			if (this.doAddStatement(statement)) {
				addedStatements.add(statement);
			}
		}

		this.notifyListenersOnStatementsAdded(addedStatements);
	}

	/**
	 * Removes a single statement from the knowledge base.
	 *
	 * @param statement the statement to remove
	 */
	public void removeStatement(final Statement statement) {
		if (this.doRemoveStatement(statement)) {
			this.notifyListenersOnStatementRemoved(statement);
		}
	}

	/**
	 * Removes a single statement from the knowledge base.
	 *
	 * @param statement the statement to remove
	 * @return true, if the knowledge base has changed.
	 */
	boolean doRemoveStatement(final Statement statement) {
		Validate.notNull(statement, "Statement cannot be Null!");

		if (this.statements.contains(statement) && statement.accept(this.removeStatementVisitor)) {
			this.statements.remove(statement);
			return true;
		}
		return false;
	}

	/**
	 * Removes a collection of statements to the knowledge base.
	 *
	 * @param statements the statements to remove
	 */
	public void removeStatements(final Collection<? extends Statement> statements) {
		final List<Statement> removedStatements = new ArrayList<>();

		for (final Statement statement : statements) {
			if (this.doRemoveStatement(statement)) {
				removedStatements.add(statement);
			}
		}

		this.notifyListenersOnStatementsRemoved(removedStatements);
	}

	/**
	 * Removes a list of statements from the knowledge base.
	 *
	 * @param statements the statements to remove
	 */
	public void removeStatements(final Statement... statements) {
		final List<Statement> removedStatements = new ArrayList<>();

		for (final Statement statement : statements) {
			if (this.doRemoveStatement(statement)) {
				removedStatements.add(statement);
			}
		}

		this.notifyListenersOnStatementsRemoved(removedStatements);
	}

	private void notifyListenersOnStatementAdded(final Statement addedStatement) {
		for (final KnowledgeBaseListener listener : this.listeners) {
			listener.onStatementAdded(addedStatement);
		}
	}

	private void notifyListenersOnStatementsAdded(final List<Statement> addedStatements) {
		if (!addedStatements.isEmpty()) {
			for (final KnowledgeBaseListener listener : this.listeners) {
				listener.onStatementsAdded(addedStatements);
			}
		}
	}

	private void notifyListenersOnStatementRemoved(final Statement removedStatement) {
		for (final KnowledgeBaseListener listener : this.listeners) {
			listener.onStatementRemoved(removedStatement);
		}
	}

	private void notifyListenersOnStatementsRemoved(final List<Statement> removedStatements) {
		if (!removedStatements.isEmpty()) {
			for (final KnowledgeBaseListener listener : this.listeners) {
				listener.onStatementsRemoved(removedStatements);
			}
		}
	}

	/**
	 * Get the list of all rules that have been added to the knowledge base. The
	 * list is read-only and cannot be modified to add or delete rules.
	 *
	 * @return list of {@link Rule}s
	 */
	public List<Rule> getRules() {
		return this.getStatementsByType(Rule.class);
	}

	/**
	 * Get the list of all asp rules that have been added to the knowledge base. The
	 * list is read-only and cannot be modified to add or delete rules.
	 *
	 * @return list of {@link AspRule}s
	 */
	public List<AspRule> getAspRules() {
		return this.getStatementsByType(AspRule.class);
	}

	/**
	 * Get the list of all show statements that have been added to the knowledge base. The
	 * list is read-only and cannot be modified to add or delete rules.
	 *
	 * @return list of {@link ShowStatement}s
	 */
	public List<ShowStatement> getShowStatements() {
		return this.getStatementsByType(ShowStatement.class);
	}

	/**
	 * Get the list of all facts that have been added to the knowledge base. The
	 * list is read-only and cannot be modified to add or delete facts.
	 *
	 * @return list of {@link Fact}s
	 */
	public List<Fact> getFacts() {
		return this.getStatementsByType(Fact.class);
	}

	/**
	 * Get the list of all data source declarations that have been added to the
	 * knowledge base. The list is read-only and cannot be modified to add or delete
	 * facts.
	 *
	 * @return list of {@link DataSourceDeclaration}s
	 */
	public List<DataSourceDeclaration> getDataSourceDeclarations() {
		return this.getStatementsByType(DataSourceDeclaration.class);
	}

	<T> List<T> getStatementsByType(final Class<T> type) {
		final ExtractStatementsVisitor<T> visitor = new ExtractStatementsVisitor<>(type);
		for (final Statement statement : this.statements) {
			statement.accept(visitor);
		}
		return Collections.unmodifiableList(visitor.getExtractedStatements());
	}

	/**
	 * Add a single fact to the internal data structures. It is assumed that it has
	 * already been checked that this fact is not present yet.
	 *
	 * @param fact the fact to add
	 */
	void addFact(final Fact fact) {
		final Predicate predicate = fact.getPredicate();
		this.factsByPredicate.putIfAbsent(predicate, new HashSet<>());
		this.factsByPredicate.get(predicate).add(fact);
	}

	/**
	 * Removes a single fact from the internal data structure. It is assumed that it
	 * has already been checked that this fact is already present.
	 *
	 * @param fact the fact to remove
	 */
	void removeFact(final Fact fact) {
		final Predicate predicate = fact.getPredicate();
		final Set<PositiveLiteral> facts = this.factsByPredicate.get(predicate);
		facts.remove(fact);
		if (facts.isEmpty()) {
			this.factsByPredicate.remove(predicate);
		}
	}

	/**
	 * Returns all {@link Statement}s of this knowledge base.
	 *
	 * The result can be iterated over and will return statements in the original
	 * order. The collection is read-only and cannot be modified to add or delete
	 * statements.
	 *
	 * @return a collection of statements
	 */
	public Collection<Statement> getStatements() {
		return Collections.unmodifiableCollection(this.statements);
	}

	@Override
	public Iterator<Statement> iterator() {
		return Collections.unmodifiableCollection(this.statements).iterator();
	}

	Map<Predicate, Set<PositiveLiteral>> getFactsByPredicate() {
		return this.factsByPredicate;
	}

	/**
	 * Interface for a method that parses the contents of a stream into a
	 * KnowledgeBase.
	 *
	 * This is essentially
	 * {@link org.semanticweb.rulewerk.parser.RuleParser#parseInto}, but we need to
	 * avoid a circular dependency here -- this is also why we throw
	 * {@link RulewerkException} instead of
	 * {@link org.semanticweb.rulewerk.parser.ParsingException}.
	 */
	@FunctionalInterface
	public interface AdditionalInputParser {
		void parseInto(InputStream stream, KnowledgeBase kb) throws IOException, RulewerkException;
	}

	/**
	 * Import rules from a file.
	 *
	 * @param file          the file to import
	 * @param parseFunction a function that transforms a {@link KnowledgeBase} using
	 *                      the {@link InputStream}.
	 *
	 * @throws IOException              when reading {@code file} fails
	 * @throws IllegalArgumentException when {@code file} is null or has already
	 *                                  been imported
	 * @throws RulewerkException        when parseFunction throws RulewerkException
	 */
	public void importRulesFile(File file, AdditionalInputParser parseFunction)
			throws RulewerkException, IOException, IllegalArgumentException {
		Validate.notNull(file, "file must not be null");

		boolean isNewFile = this.importedFilePaths.add(file.getCanonicalPath());
		Validate.isTrue(isNewFile, "file \"" + file.getName() + "\" was already imported.");

		try (InputStream stream = new FileInputStream(file)) {
			parseFunction.parseInto(stream, this);
		}
	}

	/**
	 * Merge {@link PrefixDeclarationRegistry} into this knowledge base.
	 *
	 * @param prefixDeclarationRegistry the prefix declarations to merge.
	 *                                  Conflicting prefix names in
	 *                                  {@code prefixDeclarationRegistry} will be
	 *                                  renamed to some implementation-specific,
	 *                                  fresh prefix name.
	 */
	public void mergePrefixDeclarations(PrefixDeclarationRegistry prefixDeclarationRegistry) {
		this.prefixDeclarationRegistry.mergePrefixDeclarations(prefixDeclarationRegistry);
	}

	/**
	 * Return the base IRI.
	 *
	 * @return the base IRI, if declared, or
	 *         {@link PrefixDeclarationRegistry#EMPTY_BASE} otherwise.
	 */
	public String getBaseIri() {
		return this.prefixDeclarationRegistry.getBaseIri();
	}

	/**
	 * Return the declared prefixes.
	 *
	 * @return an iterator over all known prefixes.
	 */
	public Iterator<Entry<String, String>> getPrefixes() {
		return this.prefixDeclarationRegistry.iterator();
	}

	/**
	 * Resolve {@code prefixName} into the declared IRI.
	 *
	 * @param prefixName the prefix name to resolve, including the terminating
	 *                   colon.
	 *
	 * @throws PrefixDeclarationException when the prefix has not been declared.
	 *
	 * @return the declared IRI for {@code prefixName}.
	 */
	public String getPrefixIri(String prefixName) throws PrefixDeclarationException {
		return this.prefixDeclarationRegistry.getPrefixIri(prefixName);
	}

	/**
	 * Resolve a prefixed name into an absolute IRI. Dual to
	 * {@link KnowledgeBase#unresolveAbsoluteIri}.
	 *
	 * @param prefixedName the prefixed name to resolve.
	 *
	 * @throws PrefixDeclarationException when the prefix has not been declared.
	 *
	 * @return an absolute IRI corresponding to the prefixed name.
	 */
	public String resolvePrefixedName(String prefixedName) throws PrefixDeclarationException {
		return this.prefixDeclarationRegistry.resolvePrefixedName(prefixedName);
	}

	/**
	 * Potentially abbreviate an absolute IRI using the declared prefixes. Dual to
	 * {@link KnowledgeBase#resolvePrefixedName}.
	 *
	 * @param iri the absolute IRI to abbreviate.
	 *
	 * @return either a prefixed name corresponding to {@code iri} under the
	 *         declared prefixes, or {@code iri} if no suitable prefix is declared.
	 */
	public String unresolveAbsoluteIri(String iri) {
		return this.prefixDeclarationRegistry.unresolveAbsoluteIri(iri);
	}

	/**
	 * Serialise the KnowledgeBase to the {@link OutputStream}.
	 *
	 * @param stream the {@link OutputStream} to serialise to.
	 *
	 * @throws IOException if an I/O error occurs while writing to given output stream
	 */
	public void writeKnowledgeBase(OutputStream stream) throws IOException {
		stream.write(Serializer.getBaseAndPrefixDeclarations(this).getBytes());

		for (DataSourceDeclaration dataSource : this.getDataSourceDeclarations()) {
			stream.write(Serializer.getString(dataSource).getBytes());
			stream.write('\n');
		}

		for (Rule rule : this.getRules()) {
			stream.write(Serializer.getString(rule).getBytes());
			stream.write('\n');
		}

		for (Fact fact : this.getFacts()) {
			stream.write(Serializer.getFactString(fact).getBytes());
			stream.write('\n');
		}
	}

	/**
	 * Serialise the KnowledgeBase to the given {@link File}.
	 *
	 * @param filePath path to the file to serialise into.
	 *
	 * @throws IOException
	 */
	public void writeKnowledgeBase(String filePath) throws IOException {
		try (OutputStream stream = new FileOutputStream(filePath)) {
			this.writeKnowledgeBase(stream);
		}
	}

	/**
	 * Analyse the KnowledgeBase to find all predicates used in asp rules
	 * that are approximated, thereby being aware of negation.
	 *
	 * @return the set of predicates that are approximated
	 */
	public Set<Predicate> analyseAspRulesForApproximatedPredicates() {
		Set<Predicate> approximatedPredicates = new HashSet<>(); // set of approximated predicates
		Map<Predicate, Set<Predicate>> directDependencyMap = new HashMap<>(); // contains direct dependencies of any kind
		Map<Predicate, Set<Predicate>> negationDependencyMap = new HashMap<>(); // contains direct negative dependencies

		// get the initial dependencies
		for (AspRule rule : this.getAspRules()) {
			for (Literal literal : rule.getHeadLiterals()) {
				Predicate predicate = literal.getPredicate();

				directDependencyMap.putIfAbsent(predicate, new HashSet<>());
				negationDependencyMap.putIfAbsent(predicate, new HashSet<>());

				if (rule.requiresApproximation()) {
					approximatedPredicates.add(predicate);
				}

				for (Literal bodyLiteral : rule.getBody()) {
					if (bodyLiteral.isNegated()) {
						negationDependencyMap.get(predicate).add(bodyLiteral.getPredicate());
					}
					directDependencyMap.get(predicate).add(bodyLiteral.getPredicate());
				}
			}
		}

		// used to compute the transitive closure
		Map<Predicate, Set<Predicate>> dependencyMap = new HashMap<>(directDependencyMap);

		// compute the transitive dependencies
		boolean done = false;
		while (!done) {
			done = true;
			for (Predicate predicate : dependencyMap.keySet()) {
				Set<Predicate> currentDependencies = dependencyMap.get(predicate);
				Set<Predicate> newDependencies = new HashSet<>();
				for (Predicate dependant : currentDependencies) {
					newDependencies.addAll(dependencyMap.getOrDefault(dependant, Collections.emptySet()));
				}
				if (!currentDependencies.containsAll(newDependencies)) {
					done = false;
					currentDependencies.addAll(newDependencies);
				}
			}
		}

		// direct cycles with a negation edge requires approximation
		for (Predicate predicate : negationDependencyMap.keySet()) {
			if (approximatedPredicates.contains(predicate)) {
				continue;
			}
			for (Predicate dependent : negationDependencyMap.get(predicate)) {
				if (dependencyMap.get(dependent).contains(predicate)) {
					approximatedPredicates.add(predicate);
					break;
				}
			}
		}

		// as long as a new predicate is marked as approximated, mark all predicates as approximated that use it
		done = approximatedPredicates.size() == 0;
		while (!done) {
			done = true;
			for (Predicate predicate : dependencyMap.keySet()) {
				if (!approximatedPredicates.contains(predicate) && dependencyMap.get(predicate).stream().anyMatch(approximatedPredicates::contains)) {
					approximatedPredicates.add(predicate);
					done = false;
				}
			}
		}

		return approximatedPredicates;
	}

	/**
	 * Get the set of all constants used in rules and facts of the knowledge base.
	 *
	 * @return set of constants
	 */
	public Set<Constant> getConstants() {
		Set<Constant> constants = new HashSet<>();

		getFacts().forEach(fact -> {
			fact.getTerms().forEach(term -> {
				if (term.isConstant()) {
					constants.add((Constant) term);
				}
			});
		});

		getRules().forEach(rule -> {
			rule.getTerms().forEach(term -> {
				if (term.isConstant()) {
					constants.add((Constant) term);
				}
			});
		});

		return constants;
	}

	/**
	 * Get the set of all predicates used in rules and facts of the knowledge base.
	 *
	 * @return set of predicates
	 */
	public Set<Predicate> getPredicates() {
		Set<Predicate> predicates = new HashSet<>();

		getFacts().forEach(fact -> predicates.add(fact.getPredicate()));

		getRules().forEach(rule -> {
			rule.getHead().getLiterals().forEach(literal -> predicates.add(literal.getPredicate()));
			rule.getBody().getLiterals().forEach(literal -> predicates.add(literal.getPredicate()));
		});

		getAspRules().forEach(rule -> {
			rule.getBody().getLiterals().forEach(literal -> predicates.add(literal.getPredicate()));
			rule.getHeadLiterals().getLiterals().forEach(literal -> predicates.add(literal.getPredicate()));
		});

		return predicates;
	}

	/**
	 * Checks if the fact is part of the knowledge base.
	 *
	 * @param fact a fact
	 * @return boolean
	 */
	public boolean hasFact(Fact fact) {
		return getFactsByPredicate().getOrDefault(fact.getPredicate(), Collections.emptySet()).contains(fact);
	}
}
