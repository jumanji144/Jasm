package me.darknet.assembler.error;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a result of an operation that can have errors.
 *
 * @param <T>
 *            The type of the value.
 */
public class Result<T> {

    private final T value;
    private final List<Error> errors;

    public Result(T value, List<Error> errors) {
        this.errors = errors;
        this.value = value;
    }

    public List<Error> errors() {
        return errors;
    }

    /**
     * Returns the value of the result, the existence of the value does not depend
     * on the result being ok or not.
     *
     * @return The value of the result.
     */
    public T get() {
        return value;
    }

    /**
     * @return true if the result is ok, false if there are one or more errors.
     *
     * @see #hasErr()
     */
    public boolean isOk() {
        return !hasErr() && hasValue();
    }

    /**
     * @return true if the result has an error, false if there are no errors.
     */
    public boolean hasErr() {
        return !errors.isEmpty();
    }

    /**
     * @return true if the result has a value, false if the value is null.
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Applies the given consumer if the result is ok.
     *
     * @param consumer
     *                 The consumer to apply.
     *
     * @return This
     */
    public Result<T> ifOk(Consumer<T> consumer) {
        if (isOk()) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Applies the given consumer if the result has an error.
     *
     * @param consumer
     *                 The consumer to apply.
     *
     * @return This
     */
    public Result<T> ifErr(BiConsumer<T, List<Error>> consumer) {
        if (hasErr()) {
            consumer.accept(value, errors);
        }
        return this;
    }

    public Result<T> ifErr(Consumer<List<Error>> consumer) {
        if (hasErr()) {
            consumer.accept(errors);
        }
        return this;
    }

    /**
     * Applies the given consumer if the result is ok or an error.
     *
     * @param consumer
     *                 The consumer to apply.
     *
     * @return This
     */
    public Result<T> ifAny(BiConsumer<T, List<Error>> consumer) {
        consumer.accept(value, errors);
        return this;
    }

    public <N> Result<N> map(Function<T, N> mapper) {
        if (isOk()) {
            return Result.ok(mapper.apply(value));
        } else {
            return Result.err(errors);
        }
    }

    public <N> Result<N> flatMap(Function<T, Result<N>> mapper) {
        if (isOk()) {
            return mapper.apply(value);
        } else {
            return Result.err(errors);
        }
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, List.of());
    }

    public static <T> Result<T> err(List<Error> errors) {
        return new Result<>(null, errors);
    }

    public static <T> Result<T> err(Error error) {
        return new Result<>(null, List.of(error));
    }

    public static <T> Result<T> exception(Exception e) {
        return new Result<>(null, List.of(Error.of(e)));
    }

}
