package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.test.BinarySampleFixture;
import me.darknet.assembler.test.JvmAssemblerFixture;
import me.darknet.assembler.test.JvmCompilation;
import me.darknet.assembler.test.JvmDecompilationFixture;
import me.darknet.assembler.test.JvmDisassemblyFixture;
import me.darknet.assembler.test.JvmRoundTripFixture;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static me.darknet.assembler.TestUtils.processJvm;
import static me.darknet.assembler.test.SourceNormalization.normalize;
import static org.junit.jupiter.api.Assertions.*;

class JvmRoundTripTest {
	@ParameterizedTest
	@MethodSource("validSamples")
	void all(BinarySampleFixture.JvmTextSample sample) {
		String source = sample.read();
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.version(21);

		var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
		if (source.contains("SKIP-ROUND-TRIP-EQUALITY")) {
			return;
		}

		assertEquals(
				normalize(source),
				normalize(roundTrip.disassembledSource()),
				"There was an unexpected difference in unmodified class: " + sample.name()
		);
	}

	@Test
	@Disabled
	void kotlinSr2c() {
		String source = validSamples().stream()
				.filter(sample -> sample.name().contains("KKKSample"))
				.findFirst()
				.orElseThrow()
				.read();
		JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());
	}

	@Test
	@Disabled
	void kotlinSrc() {
		String source = validSamples().stream()
				.filter(sample -> sample.name().contains("KotlinSample"))
				.findFirst()
				.orElseThrow()
				.read();
		JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());
	}

	@Test
	void kotlin() {
		byte[] raw = BinarySampleFixture.binarySample("ExtrasConfig.sample").read();
		String source = JvmDisassemblyFixture.disassembleJvm(raw);
		JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"u000d.sample", "u000a.sample", "u0009.sample", "u2028.sample",
			"u002c.sample", "u002e.sample", "u0000.sample", "u0001.sample",
			"u0002.sample", "u0003.sample", "u0004.sample", "u0005.sample",
			"u0006.sample", "u0007.sample", "u0008.sample", "u0009.sample",
			"u000a.sample", "u000b.sample", "u000c.sample", "u000d.sample",
			"u000e.sample", "u000f.sample", "u0010.sample", "u0011.sample",
			"u0012.sample", "u0013.sample", "u0014.sample", "u2000.sample",
			"u2001.sample", "u2002.sample", "u2003.sample", "u2004.sample",
			"u2005.sample", "u2006.sample", "u2007.sample", "u2008.sample",
			"u2009.sample", "u200a.sample",
	})
	void unicodeEscapeRoundTrip(String name) {
		BinarySampleFixture.BinarySample sample = BinarySampleFixture.binarySample(name);
		String source = JvmDisassemblyFixture.disassembleJvm(sample.read());
		var roundTrip = JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());

		assertEquals(
				normalize(source),
				normalize(roundTrip.disassembledSource()),
				"There was an unexpected difference in unmodified class: " + sample.name()
		);
	}

	@Test
	void supportInfinity() {
		String source = BinarySampleFixture.jvmSample("Example-infinity.jasm").read();
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.engineProvider(ValuedJvmAnalysisEngine::new);

		var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
		assertEquals(
				normalize(source.replace("InfinityD", "Infinity").replace("+", "")),
				normalize(roundTrip.disassembledSource())
		);
	}

	@Test
	void supportInfinityInWholeNumberRepresentation() {
		byte[] raw = BinarySampleFixture.binarySample("InfinityFloat.sample").read();

		String source1 = JvmDisassemblyFixture.disassembleJvm(raw, ctx -> ctx.setForceWholeNumberRepresentation(true));
		String source2 = JvmDisassemblyFixture.disassembleJvm(raw, ctx -> ctx.setForceWholeNumberRepresentation(false));

		assertEquals(source1, source2);
	}

	@Test
	void supportNan() {
		String source = BinarySampleFixture.jvmSample("Example-nan.jasm").read();
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.engineProvider(ValuedJvmAnalysisEngine::new);

		var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
		assertEquals(normalize(source.replace("NaND", "NaN")), normalize(roundTrip.disassembledSource()));
	}

	@Test
	void handlePrimitiveWidening() {
		byte[] raw = BinarySampleFixture.binarySample("TextFormatConfig.sample").read();
		String source = JvmDisassemblyFixture.disassembleJvm(raw);
		assertTrue(source.contains("iload shortenPath"));
		assertTrue(source.contains("iload escape"));
		assertTrue(source.contains("iload maxLength"));

		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.engineProvider(ValuedJvmAnalysisEngine::new);
		var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
		assertTrue(roundTrip.disassembledSource().contains("iload shortenPath"));
		assertTrue(roundTrip.disassembledSource().contains("iload escape"));
		assertTrue(roundTrip.disassembledSource().contains("iload maxLength"));
	}

	@Test
	void kotlinVariableGarbageIHateKotlin() {
		byte[] raw = BinarySampleFixture.binarySample("KotlinVarScoping.sample").read();
		String source = JvmDisassemblyFixture.disassembleJvm(raw);
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.engineProvider(ValuedJvmAnalysisEngine::new);
		var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);

		// Seriously this language is such a disaster when you look at the generated code.
		//
		// Anyways, we shouldn't be breaking the code to the point of decompilation failures.
		// Historically this has happened due to the variable name picking in the disassembler.
		// It would pick improperly scoped variables, which when reassembled, result in invalid code
		// that decompilers couldn't handle.
		String decompileOriginal = JvmDecompilationFixture.decompile(raw);
		String decompileRound = roundTrip.compilation().requireDecompilation();
		assertFalse(decompileOriginal.contains("This method has failed to decompile"), "Original class failed to decompile, cannot test");
		assertFalse(decompileRound.contains("This method has failed to decompile"), "Round-tripped class failed to decompile, cannot test");
	}

	@Test
	void deconflictsDuplicateLocalNamesAcrossDistinctSlots() throws Throwable {
		String source = JvmDisassemblyFixture.disassembleJvm(buildDuplicateLocalNameClass());

		assertTrue(source.contains("istore dup"));
		assertTrue(source.contains("istore dup2"));
		assertTrue(source.contains("iload dup"));
		assertTrue(source.contains("iload dup2"));

		assertStableRoundTrip(source);
	}

	@Test
	void keepsReusedSlotScopesDistinctWhenNamesDiffer() throws Throwable {
		String source = JvmDisassemblyFixture.disassembleJvm(buildReusedSlotScopeClass());

		assertTrue(source.contains("istore first"));
		assertTrue(source.contains("iload first"));
		assertTrue(source.contains("istore second"));
		assertTrue(source.contains("iload second"));

		assertRoundTripRetains(source, "istore first", "iload first", "istore second", "iload second");
	}

	@Test
	void ignoresBogusLocalRangesInsteadOfTrustingThem() throws Throwable {
		String source = JvmDisassemblyFixture.disassembleJvm(buildBogusLocalRangeClass());

		assertTrue(source.contains("istore i0"));
		assertTrue(source.contains("iload i0"));
		assertFalse(source.contains("broken"));

		assertRoundTripRetains(source, "istore i0", "iload i0");
	}

	@Test
	void prefersMethodParametersOverMisleadingLocals() throws Throwable {
		String source = JvmDisassemblyFixture.disassembleJvm(buildMethodParametersPreferredClass());

		assertTrue(source.contains("parameters: { this, trusted }"));
		assertTrue(source.contains("iload trusted"));
		assertFalse(source.contains("bogus"));

		assertStableRoundTrip(source);
	}

	@Test
	void keepsInstanceParameterAnnotationIndicesAndVisibilityAligned() throws Throwable {
		String source = JvmDisassemblyFixture.disassembleJvm(buildInstanceParameterAnnotationsClass());

		assertTrue(source.contains("parameters: { this, name, count }"));
		assertTrue(source.contains("parameter-annotations"));
		assertTrue(source.contains("name: {"));
		assertTrue(source.contains("count: {"));
		assertTrue(source.contains(".visible-annotation Visible"));
		assertTrue(source.contains(".invisible-annotation Hidden"));

		assertStableRoundTrip(source);
	}

	@Test
	void disassemblyAddsBoundaryLabelsBeforeRoundTrip() throws Throwable {
		String source = JvmDisassemblyFixture.disassembleJvm(buildMissingBoundaryLabelsClass());

		assertTrue(source.contains("parameters: { value }"));
		assertTrue(source.matches("(?s).*code: \\{\\s*A:\\s*iload value\\s*ifeq B\\s*return\\s*B:\\s*return\\s*C:\\s*\\}.*"), source);

		assertStableRoundTrip(source);
	}

	@Test
	void handWrittenMethodWithoutBoundaryLabelsStillCompiles() {
		String source = """
				.super java/lang/Object
				.class public super handwritten/NoBoundaryLabels {
				    .method public test (I)V {
				        parameters: { this, value },
				        code: {
				            iload value
				            pop
				            return
				        }
				    }
				}
				""";

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, new TestJvmCompilerOptions());
		assertFalse(compilation.hasWarnings(), "Expected no warnings");

		ClassNode node = readClass(compilation.requireClassBytes());
		MethodNode method = node.methods.stream()
				.filter(candidate -> candidate.name.equals("test") && candidate.desc.equals("(I)V"))
				.findFirst()
				.orElseThrow();

		assertTrue(method.instructions.getFirst() instanceof org.objectweb.asm.tree.LabelNode);
		assertTrue(method.instructions.getLast() instanceof org.objectweb.asm.tree.LabelNode);
		assertNotNull(method.localVariables);
		assertEquals(2, method.localVariables.size());
		method.localVariables.forEach(local -> assertNotSame(local.start, local.end, local.name));
	}

	@Test
	void additiveAttributesRoundTripWithoutChangingExistingSyntax() {
		String source = """
				.deprecated
				.sourcefile "Example.java"
				.source-debug-extension "SMAP\\nExample.java"
				.super java/lang/Object
				.class public example/AttrCarrier {
				    .deprecated
				    .field public static VALUE I {value: 1}

				    .deprecated
				    .method public work ()V {
				        throws: { java/lang/Exception },
				        code: {
				            A:
				            return
				        }
				    }
				}
				""";

		assertRoundTripRetains(
				source,
				".deprecated",
				".source-debug-extension",
				"throws: { java/lang/Exception }"
		);
	}

	@Test
	void kotlinStyleLocalMetadataStillRoundTrips() throws Throwable {
		byte[] raw = Files.readAllBytes(Path.of("src/test/resources/samples/binary/MainKt.sample"));
		String source = JvmDisassemblyFixture.disassembleJvm(raw);

		assertTrue(source.contains("parameters: { args }"));

		assertRoundTripRetains(source, "parameters: { args }");
	}

	static List<BinarySampleFixture.JvmTextSample> validSamples() {
		return BinarySampleFixture.validJvmSamples();
	}

	private static void assertStableRoundTrip(String source) {
		processJvm(source, new TestJvmCompilerOptions(), result -> {
			String newPrinted = JvmDisassemblyFixture.disassembleJvm(result.representation().classFile());
			assertEquals(TestUtils.normalize(source), TestUtils.normalize(newPrinted));
		});
	}

	private static void assertRoundTripRetains(String source, String... expectedFragments) {
		processJvm(source, new TestJvmCompilerOptions(), result -> {
			String newPrinted = JvmDisassemblyFixture.disassembleJvm(result.representation().classFile());
			for (String expectedFragment : expectedFragments) {
				assertTrue(newPrinted.contains(expectedFragment), expectedFragment);
			}
		});
	}

	private static byte[] buildDuplicateLocalNameClass() {
		return buildClass("hardening/DuplicateLocalNames", cw -> {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
			mv.visitCode();
			Label start = new Label();
			Label end = new Label();
			mv.visitLabel(start);
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitVarInsn(Opcodes.ISTORE, 0);
			mv.visitInsn(Opcodes.ICONST_1);
			mv.visitVarInsn(Opcodes.ISTORE, 1);
			mv.visitVarInsn(Opcodes.ILOAD, 0);
			mv.visitVarInsn(Opcodes.ILOAD, 1);
			mv.visitInsn(Opcodes.IADD);
			mv.visitInsn(Opcodes.POP);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitLabel(end);
			mv.visitLocalVariable("dup", "I", null, start, end, 0);
			mv.visitLocalVariable("dup", "I", null, start, end, 1);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		});
	}

	private static byte[] buildReusedSlotScopeClass() {
		return buildClass("hardening/ReusedSlotScopes", cw -> {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
			mv.visitCode();
			Label start = new Label();
			Label split = new Label();
			Label end = new Label();
			mv.visitLabel(start);
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitVarInsn(Opcodes.ISTORE, 0);
			mv.visitVarInsn(Opcodes.ILOAD, 0);
			mv.visitInsn(Opcodes.POP);
			mv.visitLabel(split);
			mv.visitInsn(Opcodes.ICONST_1);
			mv.visitVarInsn(Opcodes.ISTORE, 0);
			mv.visitVarInsn(Opcodes.ILOAD, 0);
			mv.visitInsn(Opcodes.POP);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitLabel(end);
			mv.visitLocalVariable("first", "I", null, start, split, 0);
			mv.visitLocalVariable("second", "I", null, split, end, 0);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		});
	}

	private static byte[] buildBogusLocalRangeClass() {
		return buildClass("hardening/BogusLocalRanges", cw -> {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
			mv.visitCode();
			Label start = new Label();
			Label late = new Label();
			mv.visitLabel(start);
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitVarInsn(Opcodes.ISTORE, 0);
			mv.visitVarInsn(Opcodes.ILOAD, 0);
			mv.visitInsn(Opcodes.POP);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitLabel(late);
			mv.visitLocalVariable("broken", "I", null, late, late, 0);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		});
	}

	private static byte[] buildMethodParametersPreferredClass() {
		return buildClass("hardening/MethodParametersPreferred", cw -> {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "(I)V", null, null);
			mv.visitParameter("trusted", 0);
			mv.visitCode();
			Label start = new Label();
			Label end = new Label();
			mv.visitLabel(start);
			mv.visitVarInsn(Opcodes.ILOAD, 1);
			mv.visitInsn(Opcodes.POP);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitLabel(end);
			mv.visitLocalVariable("this", "Lhardening/MethodParametersPreferred;", null, start, end, 0);
			mv.visitLocalVariable("bogus", "I", null, start, end, 1);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		});
	}

	private static byte[] buildInstanceParameterAnnotationsClass() {
		return buildClass("hardening/InstanceParameterAnnotations", cw -> {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "(Ljava/lang/String;I)V", null, null);
			mv.visitParameter("name", 0);
			mv.visitParameter("count", 0);

			AnnotationVisitor visible = mv.visitParameterAnnotation(0, "LVisible;", true);
			visible.visit("value", "a");
			visible.visitEnd();

			AnnotationVisitor invisible = mv.visitParameterAnnotation(1, "LHidden;", false);
			invisible.visit("value", "b");
			invisible.visitEnd();

			mv.visitCode();
			Label start = new Label();
			Label end = new Label();
			mv.visitLabel(start);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitInsn(Opcodes.POP);
			mv.visitVarInsn(Opcodes.ILOAD, 2);
			mv.visitInsn(Opcodes.POP);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitLabel(end);
			mv.visitLocalVariable("this", "Lhardening/InstanceParameterAnnotations;", null, start, end, 0);
			mv.visitLocalVariable("wrongName", "Ljava/lang/String;", null, start, end, 1);
			mv.visitLocalVariable("wrongCount", "I", null, start, end, 2);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		});
	}

	private static byte[] buildMissingBoundaryLabelsClass() {
		return buildClass("hardening/MissingBoundaryLabels", cw -> {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "(I)V", null, null);
			mv.visitParameter("value", 0);
			mv.visitCode();
			Label jumpTarget = new Label();
			mv.visitVarInsn(Opcodes.ILOAD, 0);
			mv.visitJumpInsn(Opcodes.IFEQ, jumpTarget);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitLabel(jumpTarget);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		});
	}

	private static byte[] buildClass(String internalName, Consumer<ClassWriter> body) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);
		addDefaultConstructor(cw, internalName);
		body.accept(cw);
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static void addDefaultConstructor(ClassWriter cw, String internalName) {
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label start = new Label();
		Label end = new Label();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", "L" + internalName + ";", null, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private static ClassNode readClass(byte[] bytes) {
		ClassNode node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		return node;
	}
}
