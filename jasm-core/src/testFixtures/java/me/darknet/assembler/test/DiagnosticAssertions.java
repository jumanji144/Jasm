package me.darknet.assembler.test;

import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.error.Severity;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.util.Location;
import org.junit.jupiter.api.Assertions;

import java.util.Collection;
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

	/**
	 * Formats structured diagnostics, including source locations and messages.
	 *
	 * @param diagnostics
	 * 		The diagnostics to format.
	 *
	 * @return A human-readable representation, or {@code "<none>"} when empty.
	 */
	public static String formatDiagnostics(Collection<Diagnostic> diagnostics) {
		if (diagnostics.isEmpty()) {
			return "<none>";
		}
		StringBuilder builder = new StringBuilder();
		for (Diagnostic diagnostic : diagnostics) {
			if (builder.length() > 0) {
				builder.append('\n');
			}
			Location location = diagnostic.location();
			if (location != null) {
				builder.append(location).append(": ");
			}
			builder.append(diagnostic.message());
		}
		return builder.toString();
	}

	/**
	 * Formats structured errors without changing the legacy {@link Error}-based overload.
	 */
	public static String formatErrors(Collection<Diagnostic> errors) {
		return formatDiagnostics(errors);
	}

	/**
	 * Formats structured warnings without changing the legacy {@link Warn}-based overload.
	 */
	public static String formatWarnings(Collection<Diagnostic> warnings) {
		return formatDiagnostics(warnings);
	}

	/**
	 * Returns an outcome's value after asserting that it has no errors.
	 */
	public static <T> T requireSuccess(Outcome<T> outcome, String context) {
		Assertions.assertFalse(outcome.hasErrors(), context + "\n" + formatErrors(outcome.errors()));
		Assertions.assertTrue(outcome.hasValue(), context + "\nExpected a value");
		return outcome.requireValue();
	}

	/**
	 * Asserts that an outcome contains at least one error.
	 */
	public static void assertHasErrors(Outcome<?> outcome, String context) {
		Assertions.assertTrue(outcome.hasErrors(), context + "\nExpected errors but got none");
	}

	/**
	 * Asserts that an outcome contains an error with the given stable code.
	 */
	public static void assertHasError(Outcome<?> outcome, DiagnosticCode code, String context) {
		assertHasErrorCode(outcome.errors(), code, context);
	}

	/**
	 * Asserts that a diagnostic list contains the given code.
	 */
	public static void assertHasErrorCode(List<Diagnostic> diagnostics, DiagnosticCode code, String context) {
		Assertions.assertTrue(
				diagnostics.stream().anyMatch(diagnostic -> diagnostic.code() == code),
				context + "\nExpected a diagnostic with code " + code + " but got:\n" + formatDiagnostics(diagnostics)
		);
	}

	/**
	 * Asserts that a diagnostic list contains the given phase.
	 */
	public static void assertPhase(List<Diagnostic> diagnostics, DiagnosticPhase phase, String context) {
		Assertions.assertTrue(
				diagnostics.stream().anyMatch(diagnostic -> diagnostic.phase() == phase),
				context + "\nExpected a diagnostic from phase " + phase + " but got:\n"
						+ formatDiagnostics(diagnostics)
		);
	}

	/**
	 * Asserts that a diagnostic list contains the given severity.
	 */
	public static void assertSeverity(List<Diagnostic> diagnostics, Severity severity, String context) {
		Assertions.assertTrue(
				diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == severity),
				context + "\nExpected a diagnostic with severity " + severity + " but got:\n"
						+ formatDiagnostics(diagnostics)
		);
	}
}
