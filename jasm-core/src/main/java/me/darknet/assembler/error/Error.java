package me.darknet.assembler.error;

import me.darknet.assembler.util.Location;

/**
 * Represents an error at a location.
 */
public class Error {

    private final String message;
    private final Location location;
    private final StackTraceElement[] inCodeSource;

    public Error(String message, Location location) {
        this.message = message;
        this.location = location;
        this.inCodeSource = Thread.currentThread().getStackTrace();
    }

    public String getMessage() {
        return message;
    }

    public Location getLocation() {
        return location;
    }

    /**
     * Get a stacktrace estimate where the error was created within the parser.
     * <p>
     * Primarily used for debugging.
     *
     * @return the stacktrace
     */
    public StackTraceElement[] getInCodeSource() {
        // remove the first 3 elements
        // (1st is <init>, 2nd should be the error collector add function, 3rd is the error collector caller)
        StackTraceElement[] stackTrace = new StackTraceElement[inCodeSource.length - 3];
        System.arraycopy(inCodeSource, 3, stackTrace, 0, stackTrace.length);
        return stackTrace;
    }

    @Override
    public String toString() {
        return getLocation() == null ? getMessage() : getLocation().toString() + ": " + getMessage();
    }
}
