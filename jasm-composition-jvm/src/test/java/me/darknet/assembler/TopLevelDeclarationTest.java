package me.darknet.assembler;

import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.test.JvmAssemblerFixture;
import me.darknet.assembler.test.JvmCompilation;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * These tests are meant to test the compilation of top-level declarations such as classes, fields and methods.
 * Most of our existing tests just provide a full 'Example' class, but here we'll make the top levels fields and methods too.
 */
public class TopLevelDeclarationTest {
	private static final String OVERLAY_TYPE = "top/level/OverlayExample";

	@Test
	void compilesTopLevelFieldIntoOverlayClass() {
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(
				".field public static final answer I { value: 42 }",
				overlayOptions(OVERLAY_TYPE)
		);
		ClassNode node = readClass(compilation.requireClassBytes());

		assertFalse(compilation.hasWarnings(), "Expected no warnings");
		assertEquals(OVERLAY_TYPE, node.name);
		assertMethod(node, "<init>", "()V", constructor -> assertNotNull(constructor.instructions.getFirst()));
		assertField(node, "answer", "I", field -> {
			assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, field.access);
			assertEquals(42, field.value);
		});
	}

	@Test
	void compilesTopLevelMethodIntoOverlayClass() {
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(
				"""
				.method public static hello ()V {
				    code: {
				    A:
				        return
				    B:
				    }
				}
				""",
				overlayOptions(OVERLAY_TYPE)
		);
		ClassNode node = readClass(compilation.requireClassBytes());

		assertFalse(compilation.hasWarnings(), "Expected no warnings");
		assertEquals(OVERLAY_TYPE, node.name);
		assertMethod(node, "<init>", "()V", constructor -> assertNotNull(constructor.instructions.getFirst()));
		assertMethod(node, "hello", "()V", method -> {
			assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, method.access);
			assertNotNull(method.instructions.getFirst(), "Expected method instructions");
			assertNotNull(method.instructions.getLast(), "Expected terminal label");
		});
	}

	private static TestJvmCompilerOptions overlayOptions(String internalName) {
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.overlay(new JavaClassRepresentation(buildOverlayClass(internalName)));
		return options;
	}

	private static byte[] buildOverlayClass(String internalName) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);
		MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		constructor.visitCode();
		Label start = new Label();
		Label end = new Label();
		constructor.visitLabel(start);
		constructor.visitVarInsn(Opcodes.ALOAD, 0);
		constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		constructor.visitInsn(Opcodes.RETURN);
		constructor.visitLabel(end);
		constructor.visitLocalVariable("this", "L" + internalName + ";", null, start, end, 0);
		constructor.visitMaxs(0, 0);
		constructor.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static ClassNode readClass(byte[] bytes) {
		ClassNode node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		return node;
	}

	private static void assertField(ClassNode node, String name, String descriptor, Consumer<FieldNode> assertions) {
		FieldNode field = node.fields.stream()
				.filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
				.findFirst()
				.orElse(null);
		assertNotNull(field, "Expected field " + name + " " + descriptor);
		assertions.accept(field);
	}

	private static void assertMethod(ClassNode node, String name, String descriptor, Consumer<MethodNode> assertions) {
		MethodNode method = node.methods.stream()
				.filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
				.findFirst()
				.orElse(null);
		assertNotNull(method, "Expected method " + name + " " + descriptor);
		assertions.accept(method);
	}
}
