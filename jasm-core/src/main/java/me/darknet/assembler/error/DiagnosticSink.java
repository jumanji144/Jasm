package me.darknet.assembler.error;

import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collects diagnostics and exposes severity and phase queries.
 */
public final class DiagnosticSink {
	private final List<Diagnostic> diagnostics = new ArrayList<>();
	private final DiagnosticPhase defaultPhase;

	/**
	 * Creates a sink whose diagnostics default to {@link DiagnosticPhase#TARGET_VALIDATION}.
	 */
	public DiagnosticSink() {
		this(DiagnosticPhase.TARGET_VALIDATION);
	}

	/**
	 * @param defaultPhase
	 * 		The phase applied to diagnostics added without an explicit phase.
	 */
	public DiagnosticSink(@NotNull DiagnosticPhase defaultPhase) {
		this.defaultPhase = defaultPhase;
	}

	/**
	 * Adds a diagnostic, unless an equal one is already present.
	 *
	 * @param diagnostic
	 * 		The diagnostic to add.
	 */
	public void add(@NotNull Diagnostic diagnostic) {
		if (diagnostics.contains(diagnostic))
			return;
		diagnostics.add(diagnostic);
	}

	/**
	 * Adds every diagnostic in the given collection.
	 *
	 * @param diagnostics
	 * 		The diagnostics to add.
	 */
	public void addAll(@NotNull Collection<Diagnostic> diagnostics) {
		diagnostics.forEach(this::add);
	}

	/**
	 * @param code
	 * 		The kind of problem.
	 * @param message
	 * 		Human readable description of the problem.
	 * @param location
	 * 		Position of the primary construct, or {@code null} if the problem has no source position.
	 */
	public void error(@NotNull DiagnosticCode code, @NotNull String message, @Nullable Location location) {
		add(Diagnostic.error(defaultPhase, code, message, location));
	}

	/**
	 * @param code
	 * 		The kind of problem.
	 * @param message
	 * 		Human-readable description of the problem.
	 * @param location
	 * 		Position of the primary construct, or {@code null} if the problem has no source position.
	 */
	public void warning(@NotNull DiagnosticCode code, @NotNull String message, @Nullable Location location) {
		add(Diagnostic.warning(defaultPhase, code, message, location));
	}

	/**
	 * @param phase
	 * 		The pipeline phase that detected the problem.
	 * @param code
	 * 		The kind of problem.
	 * @param message
	 * 		Human-readable description of the problem.
	 * @param location
	 * 		Position of the primary construct, or {@code null} if the problem has no source position.
	 */
	public void error(@NotNull DiagnosticPhase phase, @NotNull DiagnosticCode code, @NotNull String message,
	                  @Nullable Location location) {
		add(Diagnostic.error(phase, code, message, location));
	}

	/**
	 * @param phase
	 * 		The pipeline phase that detected the problem.
	 * @param code
	 * 		The kind of problem.
	 * @param message
	 * 		Human-readable description of the problem.
	 * @param location
	 * 		Position of the primary construct, or {@code null} if the problem has no source position.
	 */
	public void warning(@NotNull DiagnosticPhase phase, @NotNull DiagnosticCode code, @NotNull String message,
	                    @Nullable Location location) {
		add(Diagnostic.warning(phase, code, message, location));
	}

	/**
	 * Removes diagnostics from one phase at an exact source location.
	 *
	 * <p>
	 * A {@code null} location leaves the sink unchanged. Diagnostics at another location or from another
	 * phase are retained.
	 *
	 * @param location
	 * 		The location whose diagnostics should be removed, or {@code null} to remove nothing.
	 * @param phase
	 * 		The phase whose diagnostics may be removed. Diagnostics from every other phase stay.
	 */
	public void removeAt(@Nullable Location location, @NotNull DiagnosticPhase phase) {
		if (location == null)
			return;
		diagnostics.removeIf(d -> location.equals(d.location()) && d.phase() == phase);
	}

	/**
	 * @return {@code true} if at least one {@link Severity#ERROR} diagnostic is present.
	 */
	public boolean hasErrors() {
		return diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR);
	}

	/**
	 * @return {@code true} if at least one {@link Severity#WARNING} diagnostic is present.
	 */
	public boolean hasWarnings() {
		return diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING);
	}

	/**
	 * @return An unmodifiable snapshot of every diagnostic, in the order they were added.
	 */
	public @NotNull List<Diagnostic> diagnostics() {
		return List.copyOf(diagnostics);
	}

	/**
	 * @return An unmodifiable snapshot of the {@link Severity#ERROR} diagnostics, in the order they were added.
	 */
	public @NotNull List<Diagnostic> errors() {
		return diagnostics.stream().filter(d -> d.severity() == Severity.ERROR).toList();
	}

	/**
	 * @return An unmodifiable snapshot of the {@link Severity#WARNING} diagnostics, in the order they were added.
	 */
	public @NotNull List<Diagnostic> warnings() {
		return diagnostics.stream().filter(d -> d.severity() == Severity.WARNING).toList();
	}

	/**
	 * @return The phase applied to diagnostics added without an explicit phase.
	 */
	public @NotNull DiagnosticPhase defaultPhase() {
		return defaultPhase;
	}
}
