package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.definitions.instructions.Return;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import me.darknet.dex.tree.type.Types;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that DEX debug local names are emitted only when their JASM spelling is safe and lossless.
 */
class DalvikDebugNamePrintingTest {
    @Test
    void invalidDebugNameStaysNumericAndRoundTrips() {
        MethodMember method = method("invalid", "()V", code(1,
                List.of(new ConstInstruction(0, 1), new ReturnInstruction()),
                local(0, "<unused var>")));

        String printed = print(method);
        assertFalse(printed.contains("<unused var>"), printed);
        assertTrue(printed.contains("const v0"), printed);
        compile(printed);
    }

    @Test
    void narrowDebugNameIsPrintedAndRoundTrips() {
        MethodMember method = method("narrow", "()V", code(1,
                List.of(new ConstInstruction(0, 1), new ReturnInstruction()),
                local(0, "value")));

        String printed = print(method);
        assertTrue(printed.contains("const value"), printed);
        compile(printed);
    }

    @Test
    void firstDebugNameWinsAndWideNamesStayNumeric() {
        MethodMember method = method("locals", "()V", code(4,
                List.of(new ConstInstruction(0, 1),
                        new ConstWideInstruction(1, 0L),
                        new ConstInstruction(3, 2),
                        new ReturnInstruction(1, Return.WIDE)),
                local(0, "first"), local(0, "renamed"),
                local(1, "wide"), local(3, "narrow")));

        String printed = print(method);
        assertTrue(printed.contains("const first"), printed);
        assertFalse(printed.contains("renamed"), printed);
        assertFalse(printed.contains(" wide"), printed);
        assertTrue(printed.contains("const narrow"), printed);
        compile(printed);
    }

    @Test
    void wideInvokeArgumentNamesStayNumeric() {
        MethodMember method = method("invoke", "()V", new Code(0, 2, 2));
        method.getCode().addInstruction(new InvokeInstruction(
                Opcodes.INVOKE_STATIC,
                Types.instanceTypeFromInternalName("java/lang/Math"),
                "abs",
                Types.methodTypeFromDescriptor("(J)J"),
                0, 1
        ));
        method.getCode().addInstruction(new ReturnInstruction());
        method.getCode().setDebugInfo(new DebugInformation(List.of(), List.of(), List.of(
                local(0, "wideArgument"), local(1, "wideTail")
        )));

        String printed = print(method);
        assertFalse(printed.contains("wideArgument"), printed);
        assertFalse(printed.contains("wideTail"), printed);
        assertTrue(printed.contains("invoke-static"), printed);
        assertTrue(printed.contains("v0"), printed);
        assertTrue(printed.contains("v1"), printed);
        compile(printed);
    }

    @Test
    void widePolymorphicArgumentsStayNumericInListAndRangeForms() {
        MethodMember method = method("polymorphic", "()V", new Code(0, 3, 3));
        var owner = Types.instanceTypeFromInternalName("java/lang/invoke/MethodHandle");
        var declaredType = Types.methodTypeFromDescriptor("([Ljava/lang/Object;)Ljava/lang/Object;");
        var callSiteType = Types.methodTypeFromDescriptor("(J)I");
        method.getCode().addInstruction(InvokeInstruction.polymorphic(
                owner, "invokeExact", declaredType, callSiteType, 0, 1, 2));
        method.getCode().addInstruction(InvokeInstruction.polymorphicRange(
                owner, "invokeExact", declaredType, callSiteType, 3, 0));
        method.getCode().addInstruction(new ReturnInstruction());
        method.getCode().setDebugInfo(new DebugInformation(List.of(), List.of(), List.of(
                local(0, "receiver"), local(1, "wideHead"), local(2, "wideTail")
        )));

        String printed = print(method);
        assertFalse(printed.contains("wideHead"), printed);
        assertFalse(printed.contains("wideTail"), printed);
        assertTrue(printed.contains("invoke-polymorphic"), printed);
        assertTrue(printed.contains("receiver"), printed);
        assertTrue(printed.contains("v1"), printed);
        assertTrue(printed.contains("v2"), printed);
        compile(printed);
    }

    @Test
    void invalidParameterDebugNameUsesAliasAndKeepsAnnotation() {
        MethodMember method = method("annotated", "(I)V", new Code(1, 0, 1));
        method.getCode().setDebugInfo(new DebugInformation(List.of(), List.of("<unused var>"), List.of()));
        method.setParameterAnnotations(List.of(List.of(new Annotation(
                (byte) Annotation.VISIBILITY_RUNTIME,
                new AnnotationPart(Types.instanceTypeFromInternalName("java/lang/Deprecated"), Map.of())
        ))));

        String printed = print(method);
        assertTrue(printed.contains("parameters:"), printed);
        assertTrue(printed.contains("p0"), printed);
        assertTrue(printed.contains("parameter-annotations"), printed);
        assertFalse(printed.contains("<unused var>"), printed);
        ClassDefinition compiled = compile(printed);
        assertEquals(1, compiled.getMethod("annotated", "(I)V").getParameterAnnotations().get(0).size());
    }

    @Test
    void invalidParameterAliasCollisionKeepsUnnamedFallback() {
        MethodMember method = method("collision", "(II)V", new Code(2, 0, 2));
        method.getCode().setDebugInfo(new DebugInformation(
                List.of(), List.of("<unused var>", "p0"), List.of()));

        String printed = print(method);
        assertFalse(printed.contains("parameters:"), printed);
        compile(printed);
    }

    private static String print(MethodMember method) {
        ClassDefinition definition = new ClassDefinition(
                Types.instanceTypeFromInternalName("Example"),
                Types.OBJECT,
                DalvikModifiers.ACC_PUBLIC
        );
        definition.putMethod(method);
        return DalvikDexFixture.print(definition);
    }

    private static MethodMember method(String name, String descriptor, Code code) {
        MethodMember method = new MethodMember(name, Types.methodTypeFromDescriptor(descriptor),
                DalvikModifiers.ACC_PUBLIC | DalvikModifiers.ACC_STATIC);
        method.setCode(code);
        return method;
    }

    private static Code code(int registers,
                             List<Instruction> instructions,
                             DebugInformation.LocalVariable... locals) {
        Code code = new Code(0, 0, registers);
        code.addInstructions(instructions);
        code.setDebugInfo(new DebugInformation(List.of(), List.of(), List.of(locals)));
        return code;
    }

    private static DebugInformation.LocalVariable local(int register, String name) {
        return new DebugInformation.LocalVariable(register, name, null, null, null, null);
    }

    private static ClassDefinition compile(String source) {
        AtomicReference<ClassDefinition> compiled = new AtomicReference<>();
        TestUtils.processDalvik(source, TestUtils.options(), result ->
                compiled.set(((DalvikClassRepresentation) result.representation()).definition()));
        return compiled.get();
    }
}
