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

import static org.junit.Assert.assertEquals;
import static org.semanticweb.vlog4j.core.reasoner.implementation.FileDataSourceTestUtils.INPUT_FOLDER;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class CsvFileDataSourceTest {

	@Test(expected = NullPointerException.class)
	public void testConstructorNullFile() throws IOException {
		new CsvFileDataSource(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorFalseExtension() throws IOException {
		new CsvFileDataSource(new File(INPUT_FOLDER + "file.nt"));
	}

	@Test
	public void testConstructor() throws IOException {
		final CsvFileDataSource unzippedCsvFileDataSource = new CsvFileDataSource(new File(INPUT_FOLDER + "file.csv"));
		final CsvFileDataSource zippedCsvFileDataSource = new CsvFileDataSource(new File(INPUT_FOLDER + "file.csv.gz"));

		FileDataSourceTestUtils.testConstructorUnzipped(unzippedCsvFileDataSource);
		FileDataSourceTestUtils.testConstructorZipped(zippedCsvFileDataSource);
	}

	@Test
	public void testToConfigString() throws IOException {
		final CsvFileDataSource unzippedCsvFileDataSource = new CsvFileDataSource(new File(INPUT_FOLDER + "file.csv"));
		final CsvFileDataSource zippedCsvFileDataSource = new CsvFileDataSource(new File(INPUT_FOLDER + "file.csv.gz"));

		FileDataSourceTestUtils.testToConfigString(unzippedCsvFileDataSource, zippedCsvFileDataSource);
	}

	@Test
	public void testNoParentDir() throws IOException {
		final File file = new File("file.csv");
		final FileDataSource fileDataSource = new CsvFileDataSource(file);
		final String dirCanonicalPath = fileDataSource.getDirCanonicalPath();
		final String currentFolder = new File(".").getCanonicalPath();
		assertEquals(currentFolder, dirCanonicalPath);
	}

	@Test
	public void testNotNormalisedParentDir() throws IOException {
		final File file = new File("./././file.csv");
		final FileDataSource fileDataSource = new CsvFileDataSource(file);
		final String dirCanonicalPath = fileDataSource.getDirCanonicalPath();
		final String currentFolder = new File(".").getCanonicalPath();
		assertEquals(currentFolder, dirCanonicalPath);
	}

}
