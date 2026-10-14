package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies human-readable and lossless floating-point disassembly on the Dalvik backend.
 */
class DalvikFloatPrintingTest {
    private static final int FLOAT_NAN_BITS = 0x7FC00001;
    private static final long DOUBLE_NAN_BITS = 0x7FF8000000000001L;

    private static final String SOURCE = """
            .class public Example {
                .method public static floatValue ()F {
                    registers: 1,
                    code: {
                        const v0 #0x7FC00001
                        return v0
                    }
                }
                .method public static doubleValue ()D {
                    registers: 2,
                    code: {
                        const-wide v0 #0x7FF8000000000001
                        return-wide v0
                    }
                }
            }
            """;

    @Test
    void losslessHexRoundTripPreservesNaNPayloadBits() {
        ClassDefinition original = compile(SOURCE);
        String printed = print(original, PrintContext.FloatPrintMode.HEX);

        assertTrue(printed.contains("#0x7FC00001"), printed);
        assertTrue(printed.contains("#0x7FF8000000000001"), printed);
        ClassDefinition reread = compile(printed);
        assertEquals(FLOAT_NAN_BITS, floatBits(reread));
        assertEquals(DOUBLE_NAN_BITS, doubleBits(reread));
    }

    @Test
    void defaultHumanReadableRoundTripCanonicalizesNaNPayloadBits() {
        ClassDefinition original = compile(SOURCE);
        String printed = print(original, PrintContext.FloatPrintMode.STANDARD);

        assertTrue(printed.contains("NaNF"), printed);
        assertTrue(printed.contains("NaN"), printed);
        ClassDefinition reread = compile(printed);
        assertNotEquals(FLOAT_NAN_BITS, floatBits(reread));
        assertNotEquals(DOUBLE_NAN_BITS, doubleBits(reread));
    }

    @Test
    void binaryModeUsesFixedWidthRawBits() {
        ClassDefinition original = compile(SOURCE);
        String printed = print(original, PrintContext.FloatPrintMode.BINARY);

        assertTrue(printed.contains("#0b01111111110000000000000000000001"), printed);
        assertTrue(printed.contains("#0b0111111111111000000000000000000000000000000000000000000000000001"), printed);
    }

    private static ClassDefinition compile(String source) {
        AtomicReference<ClassDefinition> definition = new AtomicReference<>();
        TestUtils.processDalvik(source, TestUtils.options(), result ->
                definition.set(((DalvikClassRepresentation) result.representation()).definition()));
        return definition.get();
    }

    private static String print(ClassDefinition definition, PrintContext.FloatPrintMode mode) {
        PrintContext<?> context = new PrintContext<>("    ");
        context.setFloatPrintMode(mode);
        new DalvikClassPrinter(definition).print(context);
        return context.toString();
    }

    private static int floatBits(ClassDefinition definition) {
        ConstInstruction instruction = (ConstInstruction) definition.getMethod("floatValue", "()F")
                .getCode().getInstructions().stream()
                .filter(ConstInstruction.class::isInstance)
                .findFirst().orElseThrow();
        return instruction.value();
    }

    private static long doubleBits(ClassDefinition definition) {
        ConstWideInstruction instruction = (ConstWideInstruction) definition.getMethod("doubleValue", "()D")
                .getCode().getInstructions().stream()
                .filter(ConstWideInstruction.class::isInstance)
                .findFirst().orElseThrow();
        return instruction.value();
    }
}
