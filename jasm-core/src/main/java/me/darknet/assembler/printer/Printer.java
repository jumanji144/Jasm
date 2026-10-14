package me.darknet.assembler.printer;

/**
 * Common contract for types that emit source through a {@link PrintContext}.
 */
public interface Printer {
    /**
     * Writes this element to the supplied print context.
     *
     * @param ctx
     * 		Context receiving the emitted source.
     */
    void print(PrintContext<?> ctx);
}
