package me.darknet.assembler.test;

import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.error.Severity;
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
	 * Formats structured error diagnostics.
	 */
	public static String formatErrors(Collection<Diagnostic> errors) {
		return formatDiagnostics(errors);
	}

	/**
	 * Formats structured warning diagnostics.
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
