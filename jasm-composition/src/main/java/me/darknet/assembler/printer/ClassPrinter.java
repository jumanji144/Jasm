package me.darknet.assembler.printer;

import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.ClassFileView;
import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.type.InstanceType;
import me.darknet.assembler.JasmInterface;
import me.darknet.assembler.printer.util.Modifiers;

import java.io.IOException;
import java.io.InputStream;

public class ClassPrinter implements Printer {

	protected ClassFileView view;

	public ClassPrinter(InputStream stream) throws IOException {
		ClassBuilder builder = ClassBuilder.builder();
		JasmInterface.LIBRARY.read(stream, builder);
		view = builder.build();
	}

	@Override
	public void print(PrintContext<?> ctx) {
		var superClass = view.superClass();
		if(superClass != null)
			ctx.begin().element(".super").print(superClass.internalName()).end();
		for (InstanceType anInterface : view.interfaces()) {
			ctx.begin().element(".implements").print(anInterface.internalName()).end();
		}
		var obj = ctx.begin()
					.element(".class")
					.print(Modifiers.modifiers(view.accessFlags(), Modifiers.CLASS))
					.element(view.type().internalName())
					.declObject(view.methods().size())
					.newline();
		for (Method method : view.methods()) {
			MethodPrinter printer = new MethodPrinter(method);
			printer.print(obj.value());
			obj.next();
		}
		for (Field field : view.fields()) {
			FieldPrinter printer = new FieldPrinter(field);
			printer.print(obj.value());
			obj.next();
		}
		obj.end();
	}

}
