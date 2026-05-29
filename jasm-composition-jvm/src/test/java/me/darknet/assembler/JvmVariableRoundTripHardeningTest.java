package me.darknet.assembler;

import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static me.darknet.assembler.TestUtils.normalize;
import static me.darknet.assembler.TestUtils.processJvm;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmVariableRoundTripHardeningTest {
    @Test
    void deconflictsDuplicateLocalNamesAcrossDistinctSlots() throws Throwable {
        String source = disassemble(buildDuplicateLocalNameClass());

        assertTrue(source.contains("istore dup"));
        assertTrue(source.contains("istore dup2"));
        assertTrue(source.contains("iload dup"));
        assertTrue(source.contains("iload dup2"));

        assertStableRoundTrip(source);
    }

    @Test
    void keepsReusedSlotScopesDistinctWhenNamesDiffer() throws Throwable {
        String source = disassemble(buildReusedSlotScopeClass());

        assertTrue(source.contains("istore first"));
        assertTrue(source.contains("iload first"));
        assertTrue(source.contains("istore second"));
        assertTrue(source.contains("iload second"));

        assertRoundTripRetains(source, "istore first", "iload first", "istore second", "iload second");
    }

    @Test
    void ignoresBogusLocalRangesInsteadOfTrustingThem() throws Throwable {
        String source = disassemble(buildBogusLocalRangeClass());

        assertTrue(source.contains("istore i0"));
        assertTrue(source.contains("iload i0"));
        assertFalse(source.contains("broken"));

        assertRoundTripRetains(source, "istore i0", "iload i0");
    }

    @Test
    void prefersMethodParametersOverMisleadingLocals() throws Throwable {
        String source = disassemble(buildMethodParametersPreferredClass());

        assertTrue(source.contains("parameters: { this, trusted }"));
        assertTrue(source.contains("iload trusted"));
        assertFalse(source.contains("bogus"));

        assertStableRoundTrip(source);
    }

    @Test
    void keepsInstanceParameterAnnotationIndicesAndVisibilityAligned() throws Throwable {
        String source = disassemble(buildInstanceParameterAnnotationsClass());

        assertTrue(source.contains("parameters: { this, name, count }"));
        assertTrue(source.contains("parameter-annotations"));
        assertTrue(source.contains("name: {"));
        assertTrue(source.contains("count: {"));
        assertTrue(source.contains(".visible-annotation Visible"));
        assertTrue(source.contains(".invisible-annotation Hidden"));

        assertStableRoundTrip(source);
    }

    @Test
    void kotlinStyleLocalMetadataStillRoundTrips() throws Throwable {
        byte[] raw = Files.readAllBytes(Path.of("src/test/resources/samples/binary/MainKt.sample"));
        String source = disassemble(raw);

        assertTrue(source.contains("parameters: { args }"));

        assertRoundTripRetains(source, "parameters: { args }");
    }

    private static void assertStableRoundTrip(String source) {
        processJvm(source, new TestJvmCompilerOptions(), result -> {
            String newPrinted = disassemble(result.representation().classFile());
            assertEquals(normalize(source), normalize(newPrinted));
        });
    }

    private static void assertRoundTripRetains(String source, String... expectedFragments) {
        processJvm(source, new TestJvmCompilerOptions(), result -> {
            String newPrinted = disassemble(result.representation().classFile());
            for (String expectedFragment : expectedFragments) {
                assertTrue(newPrinted.contains(expectedFragment), expectedFragment);
            }
        });
    }

    private static String disassemble(byte[] raw) throws IOException {
        JvmClassPrinter printer = new JvmClassPrinter(raw);
        PrintContext<?> ctx = new PrintContext<>("    ");
        printer.print(ctx);
        return ctx.toString();
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
}
