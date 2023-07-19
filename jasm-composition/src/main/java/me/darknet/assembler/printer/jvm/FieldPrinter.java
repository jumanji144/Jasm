package me.darknet.assembler.printer.jvm;

import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.constant.Constant;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;

public class FieldPrinter implements Printer {

	protected Field field;
	protected MemberPrinter memberPrinter;

	public FieldPrinter(Field field) {
		this.field = field;
		this.memberPrinter = new MemberPrinter(field, MemberPrinter.Type.FIELD);
	}

	@Override
	public void print(PrintContext<?> ctx) {
		memberPrinter.printAttributes(ctx);
		memberPrinter.printDeclaration(ctx)
				.element(field.name())
				.element(field.type().descriptor());
		Constant constant = field.defaultValue();
		if(constant != null) {
			constant.accept(new ConstantPrinter(ctx));
		}
		ctx.end();
	}

}
