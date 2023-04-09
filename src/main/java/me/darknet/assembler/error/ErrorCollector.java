package me.darknet.assembler.error;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector {

	private final List<Error> errors = new ArrayList<>();

	public void addError(Error error) {
		errors.add(error);
	}

	public List<Error> getErrors() {
		return errors;
	}

}
