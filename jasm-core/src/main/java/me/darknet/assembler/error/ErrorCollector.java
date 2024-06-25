package me.darknet.assembler.error;

import me.darknet.assembler.util.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collector for errors.
 */
public class ErrorCollector {

    private final List<Error> errors = new ArrayList<>();
    private final List<Warn> warns = new ArrayList<>();

    public void addError(Error error) {
        errors.add(error);
    }

    public void addWarn(Warn warn) {
        warns.add(warn);
    }

    public void addError(String message, Location location) {
        errors.add(new Error(message, location));
    }

    public void addWarn(String message, Location location) {
        warns.add(new Warn(message, location));
    }

    public void addErrors(Collection<Error> errors) {
        this.errors.addAll(errors);
    }

    public void addWarnings(Collection<Warn> warns) {
        this.warns.addAll(warns);
    }

    public boolean hasErr() {
        return !errors.isEmpty();
    }

    public boolean hasWarn() {
        return !warns.isEmpty();
    }

    public List<Error> getErrors() {
        return errors;
    }

    public List<Warn> getWarns() {
        return warns;
    }

    @Override
    public String toString() {
        if (!hasErr() && !hasWarn())
            return "ErrorCollector{}";
        return "ErrorCollector{" +
                "errors=" + errors +
                ", warns=" + warns +
                '}';
    }
}
