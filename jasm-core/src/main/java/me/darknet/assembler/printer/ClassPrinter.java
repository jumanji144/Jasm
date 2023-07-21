package me.darknet.assembler.printer;

import org.jetbrains.annotations.Nullable;

public interface ClassPrinter extends AnnotationHolder, Printer {

	@Nullable MethodPrinter method(String name, String descriptor);

	@Nullable FieldPrinter field(String name, String descriptor);

}
