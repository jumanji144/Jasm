package me.darknet.assembler.printer.jvm;

import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.code.Local;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;
import me.darknet.assembler.printer.jvm.util.Modifiers;
import me.darknet.assembler.util.IndexedStraightforwardSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MethodPrinter implements Printer {

	protected Method method;

	public MethodPrinter(Method method) {
		this.method = method;
	}

	public Names localNames() {
		List<Names.Local> locals = new ArrayList<>();
		if(method.code() != null) {
			for (Local localVariable : method.code().localVariables()) {
				locals.add(new Names.Local(
						localVariable.index(),
						localVariable.start().index(),
						localVariable.end().index(),
						localVariable.name())
				);
			}
		}
		return new Names(Map.of(), locals);
	}

	@Override
	public void print(PrintContext<?> ctx) {
		var obj = ctx.begin()
					.element(".method")
					.print(Modifiers.modifiers(method.accessFlags(), Modifiers.METHOD))
					.element(method.name())
					.element(method.type().descriptor())
					.object(2);
		Names names = localNames();
		if(!names.parameters().isEmpty()) {
			var arr = obj.value("parameters").array(names.parameters().size());

			for (String value : names.parameters().values()) {
				arr.print(value).arg();
			}

			arr.end();
			obj.next();
		}
		var methodCode = method.code();
		if(methodCode != null) {
			var code = obj.value("code").code(methodCode.elements().size());
			InstructionPrinter printer = new InstructionPrinter(code, methodCode, names);
			IndexedStraightforwardSimulation simulation = new IndexedStraightforwardSimulation();
			simulation.execute(printer, method);
			code.end();
		}

		obj.end();
	}
}