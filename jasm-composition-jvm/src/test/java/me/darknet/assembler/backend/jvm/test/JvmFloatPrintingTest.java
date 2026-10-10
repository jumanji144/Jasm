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
        assertEquals(FLOAT_NAN_BITS, fieldValue(original.requireClassBytes(), "floatValue"));
        assertEquals(DOUBLE_NAN_BITS, fieldValue(original.requireClassBytes(), "doubleValue"));
        String printed = JvmDisassemblyFixture.disassembleJvm(original.requireClassBytes(),
		        context -> context.setFloatPrintMode(PrintContext.FloatPrintMode.HEX));

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
        String printed = JvmDisassemblyFixture.disassembleJvm(original.requireClassBytes(),
		        context -> context.setFloatPrintMode(PrintContext.FloatPrintMode.BINARY));

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

    /**
     * @param classFile
     * 		Complete class file bytes.
     * @param fieldName
     * 		Name of the constant field to read.
     *
     * @return Raw bits of the field's constant value, widened to {@code long} so one accessor covers both widths.
     *
     * @throws IllegalStateException
     * 		If the class declares no such field, or the field carries no constant value.
     */
    private static long fieldValue(byte[] classFile, String fieldName) {
        ClassNode node = new ClassNode();
        new ClassReader(classFile).accept(node, 0);

        FieldNode field = null;
        for (FieldNode candidate : node.fields) {
            if (candidate.name.equals(fieldName)) {
                field = candidate;
                break;
            }
        }

        if (field == null)
            throw new IllegalStateException("No field named " + fieldName + " in " + node.name);

        Object value = field.value;
        return switch (value) {
            case null -> throw new IllegalStateException("Field " + fieldName + " has no constant value");
            case Float floatValue -> Integer.toUnsignedLong(Float.floatToRawIntBits(floatValue));
            case Double doubleValue -> Double.doubleToRawLongBits(doubleValue);
            case Integer intValue -> Integer.toUnsignedLong(intValue);
            case Long longValue -> longValue;
            default -> throw new IllegalStateException(
                    "Field " + fieldName + " holds " + value.getClass().getSimpleName() + ", which has no raw bit form here");
        };
    }
}
