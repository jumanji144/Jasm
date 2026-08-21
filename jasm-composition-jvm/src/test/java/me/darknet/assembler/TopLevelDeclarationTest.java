package me.darknet.assembler;

import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.error.Warn;
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

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void requestedVersionOverridesOverlayVersion() {
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.version(21);
		options.overlay(new JavaClassRepresentation(buildOverlayClass(OVERLAY_TYPE, Opcodes.V1_8)));

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(
				".field public static final answer I { value: 42 }",
				options
		);
		ClassNode node = readClass(compilation.requireClassBytes());

		assertEquals(Opcodes.V21, node.version);
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

	@Test
	void replacingOverlayFieldPreservesOriginalPosition() {
		// We have a class with three fields:
		//  - first
		//  - middle
		//  - last
		// When we edit the 'middle' field it should remain in the middle amongst the other fields
		// and not shift to the first/last position.
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(
				".field public middle I { value: 7 }",
				overlayOptions(OVERLAY_TYPE)
		);
		ClassNode node = readClass(compilation.requireClassBytes());

		assertFalse(compilation.hasWarnings(), "Expected no warnings");
		assertEquals(List.of("first", "middle", "last"), node.fields.stream().map(field -> field.name).toList());
		assertField(node, "middle", "I", field -> assertEquals(7, field.value));
	}

	@Test
	void replacingOverlayMethodPreservesOriginalPosition() {
		// We have a class with three methods:
		//  - first
		//  - middle
		//  - last
		// When we edit the 'middle' method it should remain in the middle amongst the other methods
		// and not shift to the first/last position.
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(
				"""
				.method public middle ()V {
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
		assertEquals(List.of("<init>", "first", "middle", "last"), node.methods.stream().map(method -> method.name).toList());
		assertMethod(node, "middle", "()V", method -> assertNotNull(method.instructions.getFirst(), "Expected method instructions"));
	}

	@Test
	void untouchedOverlayMethodsAreCopiedWithoutRecomputingFrames() {
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.overlay(new JavaClassRepresentation(buildOverlayWithUntouchedMergedTypes("top/level/MissingTypesOverlay")));

		// Say for instance we have some class that has a bunch of methods.
		// You want to edit one of them, but the others have some weird control flow with merged types that would require frame recomputation.
		// We should be able to edit this one method without causing frame recomputation on the others, and thus not require the missing types to be present.
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
				options
		);
		ClassNode node = readClass(compilation.requireClassBytes());

		assertFalse(compilation.hasWarnings(), "Expected no warnings");
		assertMethod(node, "problematic", "()Ljava/lang/Object;", method -> assertNotNull(method.instructions.getFirst(), "Expected untouched overlay method to remain"));
		assertMethod(node, "hello", "()V", method -> assertNotNull(method.instructions.getFirst(), "Expected new method instructions"));
	}

	@Test
	void modifiedOverlayMethodsAreVerifiedWithoutTouchingUntouchedOverlayMethods() {
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.overlay(new JavaClassRepresentation(buildOverlayWithUntouchedMergedTypes("top/level/MissingTypesOverlay")));

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(
				"""
				.method public static hello ()I {
				    code: {
				    A:
				        iconst_1
				        return  // wrong return instruction, should be ireturn
				    B:
				    }
				}
				""",
				options
		);

		// The warning should be for the modified method, and not for the untouched overlay method that has missing types.
		Warn verifierWarning = compilation.warnings().stream()
				.filter(warning -> warning.getMessage().contains("may fail JVM verification"))
				.findFirst()
				.orElse(null);
		assertNotNull(verifierWarning, "Expected verifier warning for modified method");
		assertTrue(verifierWarning.getMessage().contains("hello()I"));

		// The problem method is untouched so any existing problems with it should be ignored here.
		assertFalse(verifierWarning.getMessage().contains("problematic()Ljava/lang/Object;"));
	}

	private static TestJvmCompilerOptions overlayOptions(String internalName) {
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.overlay(new JavaClassRepresentation(buildOverlayClass(internalName)));
		return options;
	}

	private static byte[] buildOverlayClass(String internalName) {
		return buildOverlayClass(internalName, Opcodes.V21);
	}

	private static byte[] buildOverlayClass(String internalName, int version) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);
		writer.visitField(Opcodes.ACC_PUBLIC, "first", "I", null, 1).visitEnd();
		writer.visitField(Opcodes.ACC_PUBLIC, "middle", "I", null, 2).visitEnd();
		writer.visitField(Opcodes.ACC_PUBLIC, "last", "I", null, 3).visitEnd();
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
		writeEmptyMethod(writer, "first");
		writeEmptyMethod(writer, "middle");
		writeEmptyMethod(writer, "last");
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static void writeEmptyMethod(ClassWriter writer, String name) {
		MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, name, "()V", null, null);
		method.visitCode();
		method.visitInsn(Opcodes.RETURN);
		method.visitMaxs(0, 0);
		method.visitEnd();
	}

	private static byte[] buildOverlayWithUntouchedMergedTypes(String internalName) {
		ClassWriter writer = new ClassWriter(0);
		writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);

		MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		constructor.visitCode();
		constructor.visitVarInsn(Opcodes.ALOAD, 0);
		constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		constructor.visitInsn(Opcodes.RETURN);
		constructor.visitMaxs(1, 1);
		constructor.visitEnd();

		MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "problematic", "()Ljava/lang/Object;", null, null);
		method.visitCode();
		Label fallback = new Label();
		Label join = new Label();
		method.visitInsn(Opcodes.ICONST_0);
		method.visitJumpInsn(Opcodes.IFEQ, fallback);
		method.visitTypeInsn(Opcodes.NEW, "missing/A");
		method.visitInsn(Opcodes.DUP);
		method.visitMethodInsn(Opcodes.INVOKESPECIAL, "missing/A", "<init>", "()V", false);
		method.visitJumpInsn(Opcodes.GOTO, join);
		method.visitLabel(fallback);
		method.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		method.visitTypeInsn(Opcodes.NEW, "missing/B");
		method.visitInsn(Opcodes.DUP);
		method.visitMethodInsn(Opcodes.INVOKESPECIAL, "missing/B", "<init>", "()V", false);
		method.visitLabel(join);
		method.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Object"});
		method.visitInsn(Opcodes.ARETURN);
		method.visitMaxs(2, 1);
		method.visitEnd();

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
