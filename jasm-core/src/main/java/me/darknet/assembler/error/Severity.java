package me.darknet.assembler.error;

/**
 * Severity of a {@link Diagnostic}.
 */
public enum Severity {
    /**
     * The diagnostic reports a problem that prevents the output from being produced.
     */
    ERROR,

    /**
     * The diagnostic reports a problem that does not prevent the output from being produced.
     */
    WARNING
}
