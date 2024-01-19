package me.darknet.assembler.compile.analysis;

import org.jetbrains.annotations.NotNull;

/**
 * Exception detailing why two {@link Value} instances cannot be merged.
 */
public class ValueMergeException extends Exception {
    /**
     * @param message
     *         Value merge failure reason.
     */
    public ValueMergeException(@NotNull String message) {
        super(message);
    }
}
