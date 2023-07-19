package me.darknet.assembler.printer.jvm;

import dev.xdark.blw.annotation.Annotation;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;

public class AnnotationPrinter implements Printer {

	private final Annotation annotation;

	public AnnotationPrinter(Annotation annotation) {
		this.annotation = annotation;
	}

	@Override
	public void print(PrintContext<?> ctx) {

	}
}
