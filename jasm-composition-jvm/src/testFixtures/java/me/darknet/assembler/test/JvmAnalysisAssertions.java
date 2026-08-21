package me.darknet.assembler.test;

import me.darknet.assembler.compile.analysis.AnalysisResults;
import org.junit.jupiter.api.Assertions;

/**
 * Utilities for asserting properties of JVM analysis results in tests.
 */
public final class JvmAnalysisAssertions {
	private JvmAnalysisAssertions() {}

	/**
	 * Asserts that the given compilation result contains warnings.
	 *
	 * @param compilation
	 * 		The compilation result to check for warnings.
	 */
	public static void assertCompileWarning(JvmCompilation compilation) {
		Assertions.assertTrue(compilation.hasWarnings(), "Expected warnings but found none");
	}

	/**
	 * Asserts that the given compilation result contains errors.
	 *
	 * @param compilation
	 * 		The compilation result to check for errors.
	 */
	public static void assertAnalysisFailure(JvmCompilation compilation) {
		Assertions.assertTrue(compilation.hasErrors(), "Expected analysis/compile errors but found none");
	}

	/**
	 * Asserts that the given compilation result contains a successful analysis result, and returns that result.
	 *
	 * @param compilation
	 * 		The compilation result to check for a successful analysis result.
	 *
	 * @return The successful analysis result contained in the compilation result.
	 */
	public static AnalysisResults requireFirstAnalysisResult(JvmCompilation compilation) {
		var result = compilation.requireSuccess().analysisLookup().allResults().values().iterator().next();
		Assertions.assertNotNull(result, "Expected analysis results");
		return result;
	}

	/**
	 * Asserts that the given compilation result contains a successful analysis result with no analysis failure, and returns that result.
	 *
	 * @param compilation
	 * 		The compilation result to check for a successful analysis result with no analysis failure.
	 *
	 * @return The successful analysis result with no analysis failure contained in the compilation result.
	 */
	public static AnalysisResults assertAnalysisSucceeded(JvmCompilation compilation) {
		AnalysisResults result = requireFirstAnalysisResult(compilation);
		Assertions.assertNull(result.getAnalysisFailure(), "Unexpected analysis failure: " + result.getAnalysisFailure());
		return result;
	}
}
