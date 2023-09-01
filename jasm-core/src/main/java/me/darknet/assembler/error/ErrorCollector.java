package me.darknet.assembler.error;

import me.darknet.assembler.util.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Collector for errors.
 */
public class ErrorCollector {

    private final List<Error> errors = new ArrayList<>();

    public void addError(Error error) {
        errors.add(error);
    }

    public void addError(String message, Location location) {
        errors.add(new Error(message, location));
    }

    public List<Error> getErrors() {
        return errors;
    }

}
