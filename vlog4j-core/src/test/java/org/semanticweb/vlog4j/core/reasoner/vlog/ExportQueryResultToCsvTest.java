package org.semanticweb.vlog4j.core.reasoner.vlog;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.semanticweb.vlog4j.core.reasoner.CsvFileUtils;

import karmaresearch.vlog.Atom;
import karmaresearch.vlog.EDBConfigurationException;
import karmaresearch.vlog.NotStartedException;
import karmaresearch.vlog.VLog;

public class ExportQueryResultToCsvTest {
	private static final String CSV_EXPORT_FOLDER = "src/test/data/output/";

	@Test
	public void testExportUnaryPredicateFacts() throws EDBConfigurationException, NotStartedException, IOException {
		final String[][] argsAMatrix = { { "c1" }, { "c2" } };
		final List<List<String>> expectedQueryResult = Arrays.asList(Arrays.asList("c1"), Arrays.asList("c2"));
		final VLog vLog = new VLog();
		vLog.addData("p", argsAMatrix);
		final String csvFilePath = CSV_EXPORT_FOLDER + "unaryFacts.csv";
		vLog.writeQueryResultsToCsv(new Atom("p", VLogExpressions.makeVariable("x")), csvFilePath);

		final List<List<String>> queryResult = CsvFileUtils.getCSVContent(csvFilePath);
		assertEquals(expectedQueryResult, queryResult);
	}

	@Test
	public void testExportBinaryPredicateFacts() throws EDBConfigurationException, NotStartedException, IOException {
		final String[][] argsAMatrix = { { "c1", "c2" }, { "c3", "c4" } };
		final List<List<String>> expectedQueryResult = Arrays.asList(Arrays.asList("c1", "c2"),
				Arrays.asList("c3", "c4"));
		final VLog vLog = new VLog();
		vLog.addData("p", argsAMatrix);
		final String csvFilePath = CSV_EXPORT_FOLDER + "binaryFacts.csv";
		vLog.writeQueryResultsToCsv(new Atom("p", VLogExpressions.makeVariable("x"), VLogExpressions.makeVariable("y")),
				csvFilePath);

		final List<List<String>> queryResult = CsvFileUtils.getCSVContent(csvFilePath);
		assertEquals(expectedQueryResult, queryResult);
	}

}
