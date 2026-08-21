package me.darknet.assembler.test;

import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.util.Location;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * Utilities for asserting on diagnostics (Errors and Warnings) produced during AST processing and compilation.
 */
public final class DiagnosticAssertions {
	private DiagnosticAssertions() {}

	/**
	 * @param errors
	 * 		The list of errors to format.
	 *
	 * @return A human-readable string representation of the list of errors, including their locations and messages.
	 * If the list is empty, returns the string {@code "<none>"}.
	 */
	public static String formatErrors(List<? extends Error> errors) {
		if (errors.isEmpty()) {
			return "<none>";
		}
		StringBuilder builder = new StringBuilder();
		for (Error error : errors) {
			if (builder.length() > 0) {
				builder.append('\n');
			}
			Location location = error.getLocation();
			if (location != null) {
				builder.append(location).append(": ");
			}
			builder.append(error.getMessage());
		}
		return builder.toString();
	}

	/**
	 * @param warnings
	 * 		The list of warnings to format.
	 *
	 * @return A human-readable string representation of the list of warnings, including their locations and messages.
	 * If the list is empty, returns the string {@code "<none>"}.
	 */
	public static String formatWarnings(List<Warn> warnings) {
		return formatErrors(warnings);
	}

	/**
	 * @param result
	 * 		The result to assert on, which may contain a value or errors.
	 * @param context
	 * 		A string providing context for the assertion, which will be included in the assertion failure message if the assertion fails.
	 * @param <T>
	 * 		The type of the value contained in the result.
	 *
	 * @return The value contained in the result if the result is ok, otherwise throws an assertion error.
	 */
	public static <T> T requireOk(Result<T> result, String context) {
		Assertions.assertFalse(result.hasErr(), context + "\n" + formatErrors(result.errors()));
		Assertions.assertTrue(result.hasValue(), context + "\nExpected a value");
		return result.get();
	}

	/**
	 * @param result
	 * 		The result to assert on, which may contain a value or errors.
	 * @param context
	 * 		A string providing context for the assertion, which will be included in the assertion failure message if the assertion fails.
	 */
	public static void assertHasErrors(Result<?> result, String context) {
		Assertions.assertTrue(result.hasErr(), context + "\nExpected errors but got none");
	}
}
