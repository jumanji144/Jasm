package me.darknet.assembler.error;

import java.util.List;

/**
 * Represents a result of an operation that can fail.
 * @param <T> The type of the value.
 */
public class Result<T> {

	private final T value;
	private final List<Error> errors;

	public Result(T value, List<Error> errors) {
		this.errors = errors;
		this.value = value;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public T get() {
		return value;
	}

	/**
	 * @return true if the result is ok, false if there are one or more errors.
	 * @see #isErr()
	 */
	public boolean isOk() {
		return !isErr();
	}

	/**
	 * @return true if the result is an error, false if there are no errors.
	 */
	public boolean isErr() {
		return !errors.isEmpty();
	}

}
