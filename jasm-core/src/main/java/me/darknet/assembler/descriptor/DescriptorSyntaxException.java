package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;

/**
 * Failure raised while parsing a malformed descriptor.
 */
public class DescriptorSyntaxException extends IllegalArgumentException {

    private final int index;
    private final String reason;

    /**
     * @param source
     * 		Descriptor text that failed to parse.
     * @param index
     * 		Zero-based offset of the failure, or {@code -1} when the failure has no meaningful position.
     * @param reason
     * 		Short description of what the parser expected, without a trailing period.
     */
    public DescriptorSyntaxException(@NotNull String source, int index, @NotNull String reason) {
        super(reason + (index < 0 ? "" : " at index " + index) + " in '" + source + "'");
        this.index = index;
        this.reason = reason;
    }

    /**
     * @return Reason with its position appended when the failure has one, for embedding in a
     * 		diagnostic message.
     */
    public @NotNull String detail() {
        return index < 0 ? reason : reason + " at index " + index;
    }
}
