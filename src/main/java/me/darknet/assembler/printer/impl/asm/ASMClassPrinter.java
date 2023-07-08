package me.darknet.assembler.printer.impl.asm;

import me.darknet.assembler.printer.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

public class ASMClassPrinter extends ClassPrinter implements Opcodes {

	static final Map<Integer, String> modifiers = Map.ofEntries(
			Map.entry(ACC_PUBLIC, "public"),
			Map.entry(ACC_PRIVATE, "private"),
			Map.entry(ACC_PROTECTED, "protected"),
			Map.entry(ACC_STATIC, "static"),
			Map.entry(ACC_FINAL, "final"),
			Map.entry(ACC_SUPER, "super"),
			Map.entry(ACC_INTERFACE, "interface"),
			Map.entry(ACC_ABSTRACT, "abstract"),
			Map.entry(ACC_SYNTHETIC, "synthetic"),
			Map.entry(ACC_ANNOTATION, "annotation"),
			Map.entry(ACC_ENUM, "enum"),
			Map.entry(ACC_MODULE, "module")
	);
	ClassNode node;
	ASMAnnotationHolder holder;

	public ASMClassPrinter(byte[] source) {
		super(source);
		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(source);
		reader.accept(node, 0);
		this.node = node;
		this.holder = ASMAnnotationHolder.fromClass(node);
	}

	static String modifiers(int access) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Integer, String> entry : modifiers.entrySet()) {
			if ((access & entry.getKey()) != 0) {
				builder.append(entry.getValue()).append(" ");
			}
		}
		return builder.toString();
	}

	@Override
	public AnnotationPrinter printAnnotation(int index) {
		return null;
	}

	@Override
	public FieldPrinter printField(String name, String descriptor) {
		return null;
	}

	@Override
	public MethodPrinter printMethod(String name, String descriptor) {
		MethodNode find = null;
		for (MethodNode method : node.methods) {
			if (method.name.equals(name) && method.desc.equals(descriptor)) {
				find = method;
				break;
			}
		}
		if (find == null) {
			return null;
		}
		return new ASMMethodPrinter(node, find);
	}

	@Override
	public void print(PrintContext<?> ctx) {
		// Print attributes
		// Print class
		var obj = ctx.begin()
				.element(".class")
				.print(modifiers(node.access))
				.element(node.name)
				.declObject()
				.newline();
		for (MethodNode method : node.methods) {
			MethodPrinter printer = new ASMMethodPrinter(node, method);
			printer.print(obj.value());
			obj.next();
		}
		obj.end();
	}


}
