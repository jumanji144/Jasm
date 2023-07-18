package me.darknet.assembler.printer;

import dev.xdark.blw.classfile.Field;
import me.darknet.assembler.printer.util.Modifiers;

public class FieldPrinter implements Printer {

	protected Field field;

	public FieldPrinter(Field field) {
		this.field = field;
	}

	@Override
	public void print(PrintContext<?> ctx) {
		ctx.begin()
				.element(".field")
				.print(Modifiers.modifiers(field.accessFlags(), Modifiers.FIELD))
				.element(field.name())
				.element(field.type().descriptor())
				.end();
	}

}
