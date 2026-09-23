package me.darknet.assembler.printer;

import org.jetbrains.annotations.Nullable;

/**
 * Target-neutral printer contract for a class and its members.
 */
public interface ClassPrinter extends AnnotationHolder, Printer {

    /**
     * @param name
     *     Method name.
     * @param descriptor
     *     Method descriptor.
     *
     * @return Method printer, or {@code null} when the method is not available.
     */
    @Nullable
    MethodPrinter method(String name, String descriptor);

    /**
     * @param name
     *     Field name.
     * @param descriptor
     *     Field descriptor.
     *
     * @return Field printer, or {@code null} when the field is not available.
     */
    @Nullable
    FieldPrinter field(String name, String descriptor);

}
