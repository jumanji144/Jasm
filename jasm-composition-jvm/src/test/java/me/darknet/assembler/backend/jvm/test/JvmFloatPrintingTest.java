package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.printer.PrintContext;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies human-readable and lossless floating-point disassembly on the JVM backend.
 */
class JvmFloatPrintingTest {
    private static final int FLOAT_NAN_BITS = 0x7FC00001;
    private static final long DOUBLE_NAN_BITS = 0x7FF8000000000001L;

    private static final String SOURCE = """
            .super java/lang/Object
            .class public super Example {
                .field public static final floatValue F { value: #0x7FC00001 }
                .field public static final doubleValue D { value: #0x7FF8000000000001 }
            }
            """;

    @Test
    void losslessHexRoundTripPreservesNaNPayloadBits() {
        JvmCompilation original = compile(SOURCE);
        String printed = JvmDisassemblyFixture.disassembleJvm(original.requireClassBytes(), context ->
                context.setFloatPrintMode(PrintContext.FloatPrintMode.HEX));

        assertTrue(printed.contains("#0x7FC00001"), printed);
        assertTrue(printed.contains("#0x7FF8000000000001"), printed);
        ClassNode reread = read(compile(printed).requireClassBytes());
        assertEquals(FLOAT_NAN_BITS, Float.floatToRawIntBits((Float) field(reread, "floatValue").value));
        assertEquals(DOUBLE_NAN_BITS, Double.doubleToRawLongBits((Double) field(reread, "doubleValue").value));
    }

    @Test
    void defaultHumanReadableRoundTripCanonicalizesNaNPayloadBits() {
        JvmCompilation original = compile(SOURCE);
        String printed = JvmDisassemblyFixture.disassembleJvm(original.requireClassBytes());

        assertTrue(printed.contains("NaNF"), printed);
        assertTrue(printed.contains("NaN"), printed);
        ClassNode reread = read(compile(printed).requireClassBytes());
        assertNotEquals(FLOAT_NAN_BITS, Float.floatToRawIntBits((Float) field(reread, "floatValue").value));
        assertNotEquals(DOUBLE_NAN_BITS, Double.doubleToRawLongBits((Double) field(reread, "doubleValue").value));
    }

    @Test
    void binaryModeUsesFixedWidthRawBits() {
        JvmCompilation original = compile(SOURCE);
        String printed = JvmDisassemblyFixture.disassembleJvm(original.requireClassBytes(), context ->
                context.setFloatPrintMode(PrintContext.FloatPrintMode.BINARY));

        assertTrue(printed.contains("#0b01111111110000000000000000000001"), printed);
        assertTrue(printed.contains("#0b0111111111111000000000000000000000000000000000000000000000000001"), printed);
    }

    private static JvmCompilation compile(String source) {
        return JvmAssemblerFixture.compileJvm(source, new TestJvmCompilerOptions());
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static FieldNode field(ClassNode node, String name) {
        return node.fields.stream().filter(candidate -> candidate.name.equals(name)).findFirst().orElseThrow();
    }
}
