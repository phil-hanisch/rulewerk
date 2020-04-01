package org.semanticweb.rulewerk.core.reasoner.implementation;

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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.Validate;

/**
 * A {@code FileDataSource} is an abstract implementation of a storage for fact
 * terms in a file of some format. The exact syntax of this storage is
 * determined by the individual extensions of this class.
 *
 * @author Christian Lewe
 * @author Irina Dragoste
 *
 */
public abstract class FileDataSource implements ReasonerDataSource {
	private final File file;
	private final String filePath;
	private final String fileName;
	private final String extension;
	/**
	 * The canonical path to the parent directory where the file resides.
	 */
	private final String dirCanonicalPath;
	private final String fileNameWithoutExtension;

	/**
	 * Constructor.
	 *
	 * @param filePath           path to a file that will serve as storage for fact
	 *                           terms.
	 * @param possibleExtensions a list of extensions that the files could have
	 * @throws IOException              if the path of the given {@code file} is
	 *                                  invalid.
	 * @throws IllegalArgumentException if the extension of the given {@code file}
	 *                                  does not occur in
	 *                                  {@code possibleExtensions}.
	 */
	public FileDataSource(final String filePath, final Iterable<String> possibleExtensions) throws IOException {
		Validate.notBlank(filePath, "Data source file path cannot be blank!");

		this.file = new File(filePath);
		this.filePath = filePath; // unmodified file path, necessary for correct serialisation
		this.fileName = this.file.getName();
		this.extension = getValidExtension(this.fileName, possibleExtensions);
		this.fileNameWithoutExtension = this.fileName.substring(0, this.fileName.lastIndexOf(this.extension));
		this.dirCanonicalPath = Paths.get(file.getCanonicalPath()).getParent().toString();
	}

	private String getValidExtension(final String fileName, final Iterable<String> possibleExtensions) {
		final Stream<String> extensionsStream = StreamSupport.stream(possibleExtensions.spliterator(), true);
		final Optional<String> potentialExtension = extensionsStream.filter(fileName::endsWith).findFirst();

		if (!potentialExtension.isPresent()) {
			throw new IllegalArgumentException("Expected one of the following extensions for the data source file "
					+ fileName + ": " + String.join(", ", possibleExtensions) + ".");
		}

		return potentialExtension.get();
	}

	public File getFile() {
		return this.file;
	}

	public String getPath() {
		return this.filePath;
	}

	public String getName() {
		return this.fileName;
	}

	/**
	 * Canonicalise the file path
	 *
	 * @return The canonical path to the parent directory where the file resides.
	 */
	public String getDirCanonicalPath() {
		return this.dirCanonicalPath;
	}

	/**
	 * Get the base name of the file, without an extension.
	 *
	 * @return the file basename without any extension.
	 */
	public String getFileNameWithoutExtension() {
		return this.fileNameWithoutExtension;
	}

	@Override
	public int hashCode() {
		return this.file.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FileDataSource)) {
			return false;
		}
		final FileDataSource other = (FileDataSource) obj;
		return this.file.equals(other.getFile());
	}

}
