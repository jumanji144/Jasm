package me.darknet.assembler.printer.jvm;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.code.Local;
import dev.xdark.blw.type.ClassType;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;
import me.darknet.assembler.printer.jvm.util.Modifiers;
import me.darknet.assembler.util.IndexedStraightforwardSimulation;

import java.util.ArrayList;
import java.util.HashMap;
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
		Map<Integer, String> parameterNames = new HashMap<>();
		boolean isStatic = (method.accessFlags() & AccessFlag.ACC_STATIC) != 0;
		int offset = isStatic ? 0 : 1;
		if (!isStatic) {
			parameterNames.put(0, "this");
		}
		List<ClassType> types = method.type().parameterTypes();
		for (int i = 0; i < types.size(); i++) {
			String name = null;
			// search for parameter name in local variables, first reference of the index which matches the type
			for (Names.Local local : locals) {
				if (local.index() == i + offset) {
					name = local.name();
					break;
				}
			}
			if (name == null)
				name = "p" + (i + offset);
			parameterNames.put(i + offset, name);
		}
		return new Names(parameterNames, locals);
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