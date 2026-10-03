package me.darknet.assembler.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result of an operation that may produce diagnostics.
 * <p>
 * {@link Success} carries a value without errors, {@link Partial} carries a value with at least one error,
 * and {@link Failure} carries errors without a value. The query methods expose diagnostics and value presence
 * without requiring callers to know the concrete variant.
 *
 * @param <T>
 * 		Type of the value this outcome carries, when it carries one.
 */
public sealed interface Outcome<T> {
	/**
	 * @return Every diagnostic, errors and warnings alike, in the order they were reported.
	 */
	@NotNull
	List<Diagnostic> diagnostics();

	/**
	 * @return The {@link Severity#ERROR} diagnostics.
	 */
	default @NotNull List<Diagnostic> errors() {
		return diagnostics().stream().filter(diagnostic -> diagnostic.severity() == Severity.ERROR).toList();
	}

	/**
	 * @return The {@link Severity#WARNING} diagnostics.
	 */
	default @NotNull List<Diagnostic> warnings() {
		return diagnostics().stream().filter(diagnostic -> diagnostic.severity() == Severity.WARNING).toList();
	}

	/**
	 * @return {@code true} when at least one error was reported, which is what makes the value, if any,
	 * unusable as output.
	 */
	default boolean hasErrors() {
		return diagnostics().stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.ERROR);
	}

	/**
	 * @return {@code true} when at least one warning was reported.
	 */
	default boolean hasWarnings() {
		return diagnostics().stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.WARNING);
	}

	/**
	 * @return The value, or {@code null} when this outcome is a {@link Failure}. Prefer
	 * {@link #requireValue()} where the variant already guarantees a value.
	 */
	@Nullable
	T value();

	/**
	 * @return {@code true} when this outcome carries a value, which is every variant but {@link Failure}.
	 */
	default boolean hasValue() {
		return !isFailure();
	}

	/**
	 * @return The value. This never returns {@code null}.
	 *
	 * @throws IllegalStateException
	 * 		If this outcome is a {@link Failure} and therefore has no value.
	 */
	@NotNull
	T requireValue();

	/**
	 * @return {@code true} when this outcome carries a value and reported no error.
	 */
	default boolean isSuccess() {
		return this instanceof Success<?>;
	}

	/**
	 * @return {@code true} when this outcome carries a value and also reported at least one error.
	 */
	default boolean isPartial() {
		return this instanceof Partial<?>;
	}

	/**
	 * @return {@code true} when this outcome reported at least one error and carries no value.
	 */
	default boolean isFailure() {
		return this instanceof Failure<?>;
	}

	/**
	 * @param consumer
	 * 		Consumer to invoke with the value when this outcome is a {@link Success}.
	 *
	 * @return Self.
	 */
	default Outcome<T> onSuccess(@NotNull Consumer<T> consumer) {
		if (this instanceof Success<T> success)
			consumer.accept(success.value());
		return this;
	}

	/**
	 * @param consumer
	 * 		Consumer to invoke with the value and diagnostics when this outcome is a {@link Partial}.
	 *
	 * @return Self.
	 */
	default Outcome<T> onPartial(@NotNull Consumer<Partial<T>> consumer) {
		if (this instanceof Partial<T> partial)
			consumer.accept(partial);
		return this;
	}

	/**
	 * @param consumer
	 * 		Consumer to invoke with the diagnostics when this outcome is a {@link Failure}.
	 *
	 * @return Self.
	 */
	default Outcome<T> onFailure(@NotNull Consumer<Failure<T>> consumer) {
		if (this instanceof Failure<T> failure)
			consumer.accept(failure);
		return this;
	}

	/**
	 * @param exceptionFactory
	 * 		Function to invoke with the diagnostics when this outcome is a {@link Failure}, which produces an exception to throw.
	 *
	 * @return Self.
	 *
	 * @throws Throwable
	 * 		The exception produced by {@code exceptionFactory} when this outcome is a {@link Failure}.
	 */
	default Outcome<T> onFailureThrow(@NotNull Function<Failure<T>, ? extends Throwable> exceptionFactory) throws Throwable {
		if (this instanceof Failure<T> failure)
			throw exceptionFactory.apply(failure);
		return this;
	}

	/**
	 * @param mapper
	 * 		Function to invoke with the value when this outcome is a {@link Success} or {@link Partial}.
	 *
	 * @return The outcome returned by {@code mapper}, or self when this outcome is a {@link Failure}.
	 */
	default Outcome<T> mapValue(@NotNull Function<T, Outcome<T>> mapper) {
		if (this instanceof Success<T> success) {
			return mapper.apply(success.value());
		} else if (this instanceof Partial<T> partial) {
			return mapper.apply(partial.value());
		}
		return this;
	}

	/**
	 * @param mapper
	 * 		Function to invoke with the value when this outcome is a {@link Success} or {@link Partial}.
	 * @param failureMapper
	 * 		Function to invoke with the diagnostics when this outcome is a {@link Failure}.
	 * @param <V>
	 * 		Type of the value the returned outcome carries, when it carries one.
	 *
	 * @return The outcome returned by {@code mapper} or {@code failureMapper}, depending on the variant.
	 */
	default <V> Outcome<V> mapValueElse(@NotNull Function<T, Outcome<V>> mapper,
	                                    @NotNull Function<Failure<T>, Outcome<V>> failureMapper) {
		return switch (this) {
			case Success<T> success -> mapper.apply(success.value());
			case Partial<T> partial -> mapper.apply(partial.value());
			case Failure<T> failure -> failureMapper.apply(failure);
		};
	}

	/**
	 * Successful outcome: a value and no error.
	 *
	 * @param value
	 * 		The value the operation produced.
	 * @param warnings
	 * 		Warnings reported alongside the value, possibly empty.
	 * @param <T>
	 * 		Type of the value.
	 */
	record Success<T>(@NotNull T value, @NotNull List<Diagnostic> warnings) implements Outcome<T> {
		public Success {
			Objects.requireNonNull(value, "value");
			warnings = requireNoErrors(warnings);
		}

		@Override
		public @NotNull List<Diagnostic> diagnostics() {
			return warnings;
		}

		@Override
		public @NotNull T requireValue() {
			return value;
		}
	}

	/**
	 * Partial outcome carrying a value together with at least one error.
	 * <p>
	 * The value may be inspected for recovery or diagnostics, but it is not valid successful output.
	 *
	 * @param value
	 * 		The value the operation produced before failing.
	 * @param diagnostics
	 * 		Every diagnostic, including at least one error.
	 * @param <T>
	 * 		Type of the value.
	 */
	record Partial<T>(@NotNull T value, @NotNull List<Diagnostic> diagnostics) implements Outcome<T> {
		public Partial {
			Objects.requireNonNull(value, "value");
			diagnostics = requireErrors(diagnostics);
		}

		@Override
		public @NotNull T requireValue() {
			return value;
		}
	}

	/**
	 * Failed outcome: no value, and the errors that explain why.
	 *
	 * @param diagnostics
	 * 		Every diagnostic, including at least one error.
	 * @param <T>
	 * 		Type of the value the operation would have produced.
	 */
	record Failure<T>(@NotNull List<Diagnostic> diagnostics) implements Outcome<T> {
		public Failure {
			diagnostics = requireErrors(diagnostics);
		}

		@Override
		public @Nullable T value() {
			return null;
		}

		@Override
		public @NotNull T requireValue() {
			throw new IllegalStateException(
					"This outcome is a failure, so it has no value; check hasErrors() or the variant first");
		}
	}

	/**
	 * Classifies a value and its diagnostics as a success, partial result, or failure.
	 *
	 * <p>
	 * A value without errors is a success; a value with errors is partial; and a missing value requires at least one error.
	 *
	 * @param value
	 * 		The value the operation produced, or {@code null} when it produced none.
	 * @param diagnostics
	 * 		Every diagnostic reported.
	 * @param <T>
	 * 		Type of the value.
	 *
	 * @return The outcome the arguments describe.
	 *
	 * @throws IllegalArgumentException
	 * 		If no value was produced and no error was reported.
	 */
	static <T> @NotNull Outcome<T> of(@Nullable T value, @NotNull List<Diagnostic> diagnostics) {
		Objects.requireNonNull(diagnostics, "diagnostics");
		if (value != null) {
			return hasError(diagnostics) ? partial(value, diagnostics) : success(value, diagnostics);
		}
		if (!hasError(diagnostics)) {
			throw new IllegalArgumentException(
					"An outcome with no value must report at least one error");
		}
		return failure(diagnostics);
	}

	/**
	 * @param value
	 * 		The value the operation produced.
	 * @param <T>
	 * 		Type of the value.
	 *
	 * @return A successful outcome carrying {@code value} and no diagnostics.
	 */
	static <T> @NotNull Outcome<T> success(@NotNull T value) {
		return new Success<>(value, List.of());
	}

	/**
	 * @param value
	 * 		The value the operation produced.
	 * @param warnings
	 * 		Warnings reported alongside the value.
	 * @param <T>
	 * 		Type of the value.
	 *
	 * @return A successful outcome carrying {@code value} and {@code warnings}.
	 *
	 * @throws IllegalArgumentException
	 * 		If {@code warnings} contains an error.
	 */
	static <T> @NotNull Outcome<T> success(@NotNull T value, @NotNull List<Diagnostic> warnings) {
		return new Success<>(value, warnings);
	}

	/**
	 * @param value
	 * 		The value the operation produced before failing.
	 * @param diagnostics
	 * 		Every diagnostic, including at least one error.
	 * @param <T>
	 * 		Type of the value.
	 *
	 * @return A partial outcome carrying {@code value} and {@code diagnostics}.
	 *
	 * @throws IllegalArgumentException
	 * 		If {@code diagnostics} reports no error.
	 */
	static <T> @NotNull Outcome<T> partial(@NotNull T value, @NotNull List<Diagnostic> diagnostics) {
		return new Partial<>(value, diagnostics);
	}

	/**
	 * @param diagnostics
	 * 		Every diagnostic, including at least one error.
	 * @param <T>
	 * 		Type of the value the operation would have produced.
	 *
	 * @return A failed outcome carrying {@code diagnostics}.
	 *
	 * @throws IllegalArgumentException
	 * 		If {@code diagnostics} reports no error.
	 */
	static <T> @NotNull Outcome<T> failure(@NotNull List<Diagnostic> diagnostics) {
		return new Failure<>(diagnostics);
	}

	/**
	 * @param diagnostic
	 * 		The single error to report.
	 * @param <T>
	 * 		Type of the value the operation would have produced.
	 *
	 * @return A failed outcome carrying {@code diagnostic}.
	 *
	 * @throws IllegalArgumentException
	 * 		If {@code diagnostic} is not an error.
	 */
	static <T> @NotNull Outcome<T> failure(@NotNull Diagnostic diagnostic) {
		return new Failure<>(List.of(diagnostic));
	}

	/**
	 * @param diagnostics
	 * 		Diagnostics to inspect.
	 *
	 * @return {@code true} when {@code diagnostics} contains an error.
	 */
	private static boolean hasError(@NotNull List<Diagnostic> diagnostics) {
		return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.ERROR);
	}

	/**
	 * @param warnings
	 * 		Diagnostics a successful outcome claims carry no error.
	 *
	 * @return An immutable copy of {@code warnings}.
	 *
	 * @throws IllegalArgumentException
	 * 		If {@code warnings} contains an error, which would make the variant contradict itself.
	 */
	private static @NotNull List<Diagnostic> requireNoErrors(@NotNull List<Diagnostic> warnings) {
		Objects.requireNonNull(warnings, "warnings");
		if (hasError(warnings)) {
			throw new IllegalArgumentException("A successful outcome cannot carry an error");
		}
		return List.copyOf(warnings);
	}

	/**
	 * @param diagnostics
	 * 		Diagnostics a partial or failed outcome claims report an error.
	 *
	 * @return An immutable copy of {@code diagnostics}.
	 *
	 * @throws IllegalArgumentException
	 * 		If {@code diagnostics} contains no error, which would make the variant contradict itself.
	 */
	private static @NotNull List<Diagnostic> requireErrors(@NotNull List<Diagnostic> diagnostics) {
		Objects.requireNonNull(diagnostics, "diagnostics");
		if (!hasError(diagnostics)) {
			throw new IllegalArgumentException("A partial or failed outcome must report at least one error");
		}
		return List.copyOf(diagnostics);
	}
}
