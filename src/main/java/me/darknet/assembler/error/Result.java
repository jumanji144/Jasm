package me.darknet.assembler.error;

import me.darknet.assembler.error.ErrorCollector;

import java.util.List;

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

	public boolean isOk() {
		return !isErr();
	}

	public boolean isErr() {
		return !errors.isEmpty();
	}

}
