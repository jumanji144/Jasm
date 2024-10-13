package me.darknet.assembler.error;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollectionException extends RuntimeException {
    private final List<Error> errors = new ArrayList<>();
    private final List<Warn> warns = new ArrayList<>();
    
    public ErrorCollectionException(String message, ErrorCollector collector) {
        super(message);
        errors.addAll(collector.getErrors());
        warns.addAll(collector.getWarns());
    }

    public List<Error> getErrors() {
        return errors;
    }

    public List<Warn> getWarns() {
        return warns;
    }
}
