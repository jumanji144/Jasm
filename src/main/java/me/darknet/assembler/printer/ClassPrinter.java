package me.darknet.assembler.printer;

public abstract class ClassPrinter extends AnnotationOwner implements Printer {

	protected byte[] source;

	public ClassPrinter(byte[] source) {
		this.source = source;
	}

	public abstract FieldPrinter printField(String name, String descriptor);

	public abstract MethodPrinter printMethod(String name, String descriptor);

}
