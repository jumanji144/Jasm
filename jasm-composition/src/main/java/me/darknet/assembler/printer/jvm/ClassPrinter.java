package me.darknet.assembler.printer.jvm;

import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.ClassFileView;
import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.type.InstanceType;
import me.darknet.assembler.JasmInterface;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;

import java.io.IOException;
import java.io.InputStream;

public class ClassPrinter implements Printer {

	protected ClassFileView view;
	protected MemberPrinter memberPrinter;

	public ClassPrinter(InputStream stream) throws IOException {
		ClassBuilder builder = ClassBuilder.builder();
		JasmInterface.LIBRARY.read(stream, builder);
		view = builder.build();
		this.memberPrinter = new MemberPrinter(view, view, view, MemberPrinter.Type.CLASS);
	}

	@Override
	public void print(PrintContext<?> ctx) {
		memberPrinter.printAttributes(ctx);
		var superClass = view.superClass();
		if (superClass != null)
			ctx.begin().element(".super").print(superClass.internalName()).end();
		for (InstanceType anInterface : view.interfaces()) {
			ctx.begin().element(".implements").print(anInterface.internalName()).end();
		}
		var obj = memberPrinter.printDeclaration(ctx)
				.element(view.type().internalName())
				.declObject()
				.newline();
		for (Method method : view.methods()) {
			MethodPrinter printer = new MethodPrinter(method);
			printer.print(obj);
			obj.doubleNext();
		}
		for (Field field : view.fields()) {
			FieldPrinter printer = new FieldPrinter(field);
			printer.print(obj);
			obj.doubleNext();
		}
		obj.end();
	}

}
