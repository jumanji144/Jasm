package me.darknet.assembler.printer;

import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.simulation.StraightforwardSimulation;
import me.darknet.assembler.printer.util.Modifiers;

import java.util.List;
import java.util.Map;

public class MethodPrinter implements Printer {

	protected Method method;

	public MethodPrinter(Method method) {
		this.method = method;
	}

	public Names localNames() {
		return new Names(Map.of(), List.of());
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
			var code = obj.value("code").code(methodCode.codeList().size());
			InstructionPrinter printer = new InstructionPrinter(code, methodCode, names);
			StraightforwardSimulation simulation = new StraightforwardSimulation();
			simulation.execute(printer, method);
			code.end();
		}

		obj.end();
	}
}