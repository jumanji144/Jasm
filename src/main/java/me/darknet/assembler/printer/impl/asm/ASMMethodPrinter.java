package me.darknet.assembler.printer.impl.asm;

import me.darknet.assembler.printer.AnnotationPrinter;
import me.darknet.assembler.printer.MethodPrinter;
import me.darknet.assembler.printer.Names;
import me.darknet.assembler.printer.PrintContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASMMethodPrinter extends MethodPrinter implements Opcodes {

	ClassNode owner;
	MethodNode method;
	ASMAnnotationHolder holder;

	ASMMethodPrinter(ClassNode owner, MethodNode method) {
		this.owner = owner;
		this.method = method;
		this.holder = ASMAnnotationHolder.fromMethod(method);
	}

	@Override
	public AnnotationPrinter printAnnotation(int index) {
		return null;
	}

	public Names localNames() {
		Map<Integer, String> parameterNames = new HashMap<>();
		List<Names.Local> locals = new ArrayList<>();
		if(method.localVariables != null) {
			for (int i = 0; i < method.localVariables.size(); i++) {
				var local = method.localVariables.get(i);
				var l = new Names.Local();
				l.index = local.index;
				l.start = method.instructions.indexOf(local.start) - 1;
				l.end = method.instructions.indexOf(local.end) - 1;
				l.name = local.name;
				locals.add(l);
			}
		}
		// first collect parameter names
		boolean isStatic = (method.access & ACC_STATIC) != 0;
		int offset = isStatic ? 0 : 1;
		if(!isStatic) {
			parameterNames.put(0, "this");
		}
		Type[] types = Type.getArgumentTypes(method.desc);
		for (int i = 0; i < types.length; i++) {
			String name = null;
			if(method.parameters != null) {
				ParameterNode node = method.parameters.get(i);
				if(node != null)
					name = node.name;
			}
			if(name == null) {
				// search for parameter name in local variables, first reference of the index which matches the type
				for (Names.Local local : locals) {
					if (local.index == i + offset) {
						name = local.name;
						break;
					}
				}
			}
			if(name == null)
				name = makeName(types[i], "p", i+offset);
			parameterNames.put(i+offset, name);
		}
		return new Names(parameterNames, locals);
	}

	String makeName(Type type, String c, int index) {
		if(type.getSort() == Type.ARRAY)
			type = type.getElementType();

		if(type.getSort() < Type.INT)
			type = Type.INT_TYPE;

		String name = type.getInternalName();

		// shorten name
		int last = name.lastIndexOf('/');
		if(last != -1)
			name = name.substring(last + 1);

		// remove trailing ;
		if(name.endsWith(";"))
			name = name.substring(0, name.length() - 1);

		// lowercase first letter
		name = name.substring(0, 1).toLowerCase() + name.substring(1);

		// if name to bland, use index
		if(noOkName(name))
			name = c + index;

		return name;
	}

	boolean noOkName(String name) {
		if(name.startsWith("object")) return true;
		if(name.contains("-")) return true;
		// if the name is just numbers, it's probably not a good name
		return name.matches("[0-9]+");
	}

	@Override
	public void print(PrintContext<?> ctx) {
		var obj = ctx.begin()
				.element(".method")
				.print(modifiers(method.access))
				.element(method.name)
				.element(method.desc)
				.object();
		Names names = localNames();
		if(!names.getParameters().isEmpty()) {
			var arr = obj.value("parameters").array();

			for (String name : names.getParameters().values()) {
				arr.print(name).arg();
			}

			arr.end();
			obj.next();
		}
		var code = obj.value("code").code();
		ASMInstructionPrinter printer = new ASMInstructionPrinter(method.instructions, names);
		printer.print(code);
		code.end();
		obj.next().end();
	}

	static final Map<Integer, String> modifiers = Map.ofEntries(
			Map.entry(ACC_PUBLIC, "public"),
			Map.entry(ACC_PRIVATE, "private"),
			Map.entry(ACC_PROTECTED, "protected"),
			Map.entry(ACC_STATIC, "static"),
			Map.entry(ACC_FINAL, "final"),
			Map.entry(ACC_SYNCHRONIZED, "synchronized"),
			Map.entry(ACC_BRIDGE, "bridge"),
			Map.entry(ACC_VARARGS, "varargs"),
			Map.entry(ACC_NATIVE, "native"),
			Map.entry(ACC_ABSTRACT, "abstract"),
			Map.entry(ACC_STRICT, "strict"),
			Map.entry(ACC_SYNTHETIC, "synthetic")
	);

	static String modifiers(int access) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Integer, String> entry : modifiers.entrySet()) {
			if ((access & entry.getKey()) != 0) {
				builder.append(entry.getValue()).append(" ");
			}
		}
		return builder.toString();
	}

}
