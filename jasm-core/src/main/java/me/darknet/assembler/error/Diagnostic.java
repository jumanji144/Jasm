package me.darknet.assembler.error;

import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A problem found anywhere between lexing and output verification.
 *
 * @param severity
 * 		Whether the problem prevents output from being produced.
 * @param phase
 * 		The pipeline phase that detected the problem.
 * @param code
 * 		The kind of problem, for consumers that must not match on message text.
 * @param message
 * 		Human readable description of the problem.
 * @param location
 * 		Position of the primary construct the problem concerns. Can be {@code null} when the problem is not tied to a
 * 		source position, such as a backend invariant.
 * @param related
 * 		Additional positions that explain the problem, for example the declaration that set a limit the primary
 * 		location exceeded.
 */
public record Diagnostic(@NotNull Severity severity,
                         @NotNull DiagnosticPhase phase,
                         @NotNull DiagnosticCode code,
                         @NotNull String message,
                         @Nullable Location location,
                         @NotNull List<Location> related) {

	public Diagnostic {
		Objects.requireNonNull(severity, "severity");
		Objects.requireNonNull(phase, "phase");
		Objects.requireNonNull(code, "code");
		Objects.requireNonNull(message, "message");
		related = List.copyOf(related);
	}

	/**
	 * Creates an {@link Severity#ERROR} diagnostic with no related locations.
	 *
	 * @param phase
	 * 		The pipeline phase that detected the problem.
	 * @param code
	 * 		The kind of problem.
	 * @param message
	 * 		Human readable description of the problem.
	 * @param location
	 * 		Position of the primary construct, or {@code null} if the problem has no source position.
	 *
	 * @return The created diagnostic.
	 */
	public static @NotNull Diagnostic error(@NotNull DiagnosticPhase phase, @NotNull DiagnosticCode code,
	                                        @NotNull String message, @Nullable Location location) {
		return new Diagnostic(Severity.ERROR, phase, code, message, location, List.of());
	}

	/**
	 * Creates a {@link Severity#WARNING} diagnostic with no related locations.
	 *
	 * @param phase
	 * 		The pipeline phase that detected the problem.
	 * @param code
	 * 		The kind of problem.
	 * @param message
	 * 		Human readable description of the problem.
	 * @param location
	 * 		Position of the primary construct, or {@code null} if the problem has no source position.
	 *
	 * @return The created diagnostic.
	 */
	public static @NotNull Diagnostic warning(@NotNull DiagnosticPhase phase, @NotNull DiagnosticCode code,
	                                          @NotNull String message, @Nullable Location location) {
		return new Diagnostic(Severity.WARNING, phase, code, message, location, List.of());
	}

	/**
	 * Renders the diagnostic with its code and related locations for CLI output.
	 *
	 * @return One-line rendering including the code and any related locations.
	 */
	public @NotNull String format() {
		StringBuilder rendered = new StringBuilder();
		if (location != null)
			rendered.append(location).append(": ");

		rendered.append('[').append(code).append("] ").append(message);

		if (!related.isEmpty()) {
			rendered.append(" (related: ");
			for (int i = 0; i < related.size(); i++) {
				if (i > 0)
					rendered.append(", ");
				rendered.append(related.get(i));
			}
			rendered.append(')');
		}

		return rendered.toString();
	}

	@Override
	public @NotNull String toString() {
		return location == null ? message : location + ": " + message;
	}
}
