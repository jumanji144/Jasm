package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.instructions.Binary2AddrInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryLiteralInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the register and literal fields at the boundaries of each affected encoding. Accepted forms are observed
 * after a DEX write and read, so the decoded operands and constant bits are what is checked. A register the
 * encoding cannot name is rejected with a diagnostic that is not the declared-count diagnostic.
 */
class DalvikEncodingBoundaryTest {

    @Test
    void highRegisterConstKeepsDestinationAndValue() throws IOException {
        List<ConstInstruction> consts = decoded(decodedInstructions(example("""
                .method public static consts ()V {
                    registers: 256,
                    code: {
                        const v21 7
                        const v22 -3
                        const v255 1
                        return-void
                    }
                }
                """), "consts", "()V"), ConstInstruction.class);

        assertEquals(3, consts.size());
        assertConst(consts.get(0), 21, 7, Opcodes.CONST_16);
        assertConst(consts.get(1), 22, -3, Opcodes.CONST_16);
        assertConst(consts.get(2), 255, 1, Opcodes.CONST_16);
    }

    @Test
    void lowRegisterConstKeepsTheFourBitForm() throws IOException {
        List<ConstInstruction> consts = decoded(decodedInstructions(example("""
                .method public static consts ()V {
                    registers: 16,
                    code: {
                        const v1 7
                        const v15 -8
                        return-void
                    }
                }
                """), "consts", "()V"), ConstInstruction.class);

        assertEquals(2, consts.size());
        assertConst(consts.get(0), 1, 7, Opcodes.CONST_4);
        assertConst(consts.get(1), 15, -8, Opcodes.CONST_4);
    }

    @Test
    void highRegisterConstKeepsFloatPayloadBits() throws IOException {
        List<ConstInstruction> consts = decoded(decodedInstructions(example("""
                .method public static floats ()V {
                    registers: 256,
                    code: {
                        const v200 #0x7FC00001
                        const v100 #0x3F800000
                        return-void
                    }
                }
                """), "floats", "()V"), ConstInstruction.class);

        assertEquals(2, consts.size());
        assertConst(consts.get(0), 200, 0x7FC00001, Opcodes.CONST);
        assertConst(consts.get(1), 100, 0x3F800000, Opcodes.CONST_HIGH16);
    }

    @Test
    void literal8ArithmeticKeepsRegistersAboveV15() throws IOException {
        List<BinaryLiteralInstruction> operations = decoded(decodedInstructions(example("""
                .method public static literal8 (II)I {
                    registers: 18,
                    parameters: { a, b },
                    code: {
                        add-int v16 v17 3
                        rsub-int v17 v16 -5
                        return v16
                    }
                }
                """), "literal8", "(II)I"), BinaryLiteralInstruction.class);

        assertEquals(2, operations.size());
        assertLiteral(operations.get(0), Opcodes.ADD_INT_LIT8, 16, 17, 3);
        assertLiteral(operations.get(1), Opcodes.RSUB_INT_LIT8, 17, 16, -5);
    }

    @Test
    void literal16ArithmeticKeepsTheLastFourBitRegisters() throws IOException {
        List<BinaryLiteralInstruction> operations = decoded(decodedInstructions(example("""
                .method public static literal16 (II)I {
                    registers: 16,
                    parameters: { a, b },
                    code: {
                        add-int v14 v15 300
                        rsub-int v15 v14 -32768
                        return v14
                    }
                }
                """), "literal16", "(II)I"), BinaryLiteralInstruction.class);

        assertEquals(2, operations.size());
        assertLiteral(operations.get(0), Opcodes.ADD_INT_LIT16, 14, 15, 300);
        assertLiteral(operations.get(1), Opcodes.RSUB_INT, 15, 14, -32768);
    }

    @Test
    void twoAddressArithmeticKeepsTheLastFourBitRegisters() throws IOException {
        List<Binary2AddrInstruction> operations = decoded(decodedInstructions(example("""
                .method public static twoAddress (II)I {
                    registers: 16,
                    parameters: { a, b },
                    code: {
                        add-int v14 v14 v15
                        add-int v15 v15 v15
                        return v14
                    }
                }
                """), "twoAddress", "(II)I"), Binary2AddrInstruction.class);

        assertEquals(2, operations.size());
        assertEquals(Opcodes.ADD_INT_2ADDR, operations.get(0).opcode());
        assertEquals(14, operations.get(0).a());
        assertEquals(15, operations.get(0).b());
        assertEquals(15, operations.get(1).a());
        assertEquals(15, operations.get(1).b());
    }

    @Test
    void threeRegisterArithmeticKeepsTheLastEightBitRegister() throws IOException {
        List<BinaryInstruction> operations = decoded(decodedInstructions(example("""
                .method public static threeRegister (II)I {
                    registers: 256,
                    parameters: { a, b },
                    code: {
                        add-int v255 v254 v255
                        return v255
                    }
                }
                """), "threeRegister", "(II)I"), BinaryInstruction.class);

        assertEquals(1, operations.size());
        assertEquals(Opcodes.ADD_INT, operations.getFirst().opcode());
        assertEquals(255, operations.getFirst().dest());
        assertEquals(254, operations.getFirst().a());
        assertEquals(255, operations.getFirst().b());
    }

    @Test
    void literalAtTheEightBitBoundariesSelectsTheEightBitForm() throws IOException {
        List<BinaryLiteralInstruction> operations = decoded(decodedInstructions(example("""
                .method public static bounds ()V {
                    registers: 256,
                    code: {
                        add-int v255 v0 127
                        add-int v0 v255 -128
                        return-void
                    }
                }
                """), "bounds", "()V"), BinaryLiteralInstruction.class);

        assertEquals(2, operations.size());
        assertLiteral(operations.get(0), Opcodes.ADD_INT_LIT8, 255, 0, 127);
        assertLiteral(operations.get(1), Opcodes.ADD_INT_LIT8, 0, 255, -128);
    }

    @Test
    void literalJustOutsideTheEightBitRangeSelectsTheSixteenBitForm() throws IOException {
        List<BinaryLiteralInstruction> operations = decoded(decodedInstructions(example("""
                .method public static bounds ()V {
                    registers: 16,
                    code: {
                        add-int v0 v15 128
                        add-int v14 v1 -129
                        add-int v14 v15 32767
                        add-int v15 v14 -32768
                        return-void
                    }
                }
                """), "bounds", "()V"), BinaryLiteralInstruction.class);

        assertEquals(4, operations.size());
        assertLiteral(operations.get(0), Opcodes.ADD_INT_LIT16, 0, 15, 128);
        assertLiteral(operations.get(1), Opcodes.ADD_INT_LIT16, 14, 1, -129);
        assertLiteral(operations.get(2), Opcodes.ADD_INT_LIT16, 14, 15, 32767);
        assertLiteral(operations.get(3), Opcodes.ADD_INT_LIT16, 15, 14, -32768);
    }

    @Test
    void literalEightAddsOnAHighRegister() throws IOException {
        List<BinaryLiteralInstruction> operations = decoded(decodedInstructions(example("""
                .method public static high ()I {
                    registers: 202,
                    code: {
                        add-int v200 v100 5
                        return v200
                    }
                }
                """), "high", "()I"), BinaryLiteralInstruction.class);

        assertEquals(1, operations.size());
        assertLiteral(operations.getFirst(), Opcodes.ADD_INT_LIT8, 200, 100, 5);
    }

    @Test
    void shiftLiteralsSelectTheEightBitForm() throws IOException {
        List<BinaryLiteralInstruction> operations = decoded(decodedInstructions(example("""
                .method public static shifts (I)I {
                    registers: 18,
                    parameters: { value },
                    code: {
                        shl-int v16 value 5
                        ushr-int v17 v16 -1
                        return v17
                    }
                }
                """), "shifts", "(I)I"), BinaryLiteralInstruction.class);

        assertEquals(2, operations.size());
        assertLiteral(operations.get(0), Opcodes.SHL_INT_LIT8, 16, 17, 5);
        assertLiteral(operations.get(1), Opcodes.USHR_INT_LIT8, 17, 16, -1);
    }

    @Test
    void reverseSubtractionSelectsItsFormFromTheLiteral() throws IOException {
        List<BinaryLiteralInstruction> operations = decoded(decodedInstructions(example("""
                .method public static reverse (I)I {
                    registers: 3,
                    parameters: { value },
                    code: {
                        rsub-int v1 value -5
                        rsub-int v0 value 300
                        return v0
                    }
                }
                """), "reverse", "(I)I"), BinaryLiteralInstruction.class);

        assertEquals(2, operations.size());
        assertLiteral(operations.get(0), Opcodes.RSUB_INT_LIT8, 1, 2, -5);
        assertLiteral(operations.get(1), Opcodes.RSUB_INT, 0, 2, 300);
    }

    @Test
    void twoAddressFormIsChosenOnlyWhenTheDestinationIsTheFirstSource() throws IOException {
        List<Instruction> instructions = decodedInstructions(example("""
                .method public static inPlace ()I {
                    registers: 17,
                    code: {
                        add-int v0 v0 v1
                        add-int v0 v0 v16
                        add-int v16 v16 v1
                        return v0
                    }
                }
                """), "inPlace", "()I");

        List<Binary2AddrInstruction> compact = decoded(instructions, Binary2AddrInstruction.class);
        assertEquals(1, compact.size());
        assertEquals(Opcodes.ADD_INT_2ADDR, compact.getFirst().opcode());
        assertEquals(0, compact.getFirst().a());
        assertEquals(1, compact.getFirst().b());

        List<BinaryInstruction> ordinary = decoded(instructions, BinaryInstruction.class);
        assertEquals(2, ordinary.size());
        assertEquals(0, ordinary.get(0).dest());
        assertEquals(0, ordinary.get(0).a());
        assertEquals(16, ordinary.get(0).b());
        assertEquals(16, ordinary.get(1).dest());
        assertEquals(16, ordinary.get(1).a());
        assertEquals(1, ordinary.get(1).b());
    }

    @Test
    void aliasedDestinationIsComparedBySlotAndSubtractionKeepsItsOrder() throws IOException {
        // p0 and a name the same slot, so the in-place add takes the compact form. The subtraction's destination is
        // its second source, which must stay in the written position rather than be swapped into a compact form.
        List<Instruction> instructions = decodedInstructions(example("""
                .method public static alias (II)I {
                    registers: 2,
                    parameters: { a, b },
                    code: {
                        add-int p0 a b
                        sub-int p1 a b
                        return p0
                    }
                }
                """), "alias", "(II)I");

        List<Binary2AddrInstruction> compact = decoded(instructions, Binary2AddrInstruction.class);
        assertEquals(1, compact.size());
        assertEquals(Opcodes.ADD_INT_2ADDR, compact.getFirst().opcode());
        assertEquals(0, compact.getFirst().a());
        assertEquals(1, compact.getFirst().b());

        List<BinaryInstruction> ordinary = decoded(instructions, BinaryInstruction.class);
        assertEquals(1, ordinary.size());
        assertEquals(Opcodes.SUB_INT, ordinary.getFirst().opcode());
        assertEquals(1, ordinary.getFirst().dest());
        assertEquals(0, ordinary.getFirst().a());
        assertEquals(1, ordinary.getFirst().b());
    }

    @Test
    void wideInPlaceArithmeticUsesThePairSlotWhenItFits() throws IOException {
        List<Instruction> instructions = decodedInstructions(example("""
                .method public static wide ()J {
                    registers: 256,
                    code: {
                        add-long v0 v0 v2
                        add-long v254 v254 v0
                        return-wide v0
                    }
                }
                """), "wide", "()J");

        List<Binary2AddrInstruction> compact = decoded(instructions, Binary2AddrInstruction.class);
        assertEquals(1, compact.size());
        assertEquals(Opcodes.ADD_LONG_2ADDR, compact.getFirst().opcode());
        assertEquals(0, compact.getFirst().a());
        assertEquals(2, compact.getFirst().b());

        List<BinaryInstruction> ordinary = decoded(instructions, BinaryInstruction.class);
        assertEquals(1, ordinary.size());
        assertEquals(Opcodes.ADD_LONG, ordinary.getFirst().opcode());
        assertEquals(254, ordinary.getFirst().dest());
        assertEquals(254, ordinary.getFirst().a());
        assertEquals(0, ordinary.getFirst().b());
    }

    @Test
    void nonRangeInvokeKeepsTheFiveWordBoundary() throws IOException {
        List<InvokeInstruction> invokes = decoded(decodedInstructions(example("""
                .method public static fiveWords (IIIII)V {
                    registers: 5,
                    parameters: { a, b, c, d, e },
                    code: {
                        invoke-static { v0, v1, v2, v3, v4 } demo/Target.five (IIIII)V
                        return-void
                    }
                }
                """), "fiveWords", "(IIIII)V"), InvokeInstruction.class);

        assertEquals(1, invokes.size());
        assertFalse(invokes.getFirst().isRange());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, invokes.getFirst().arguments());
    }

    @Test
    void aHighRegisterInAShortListIsEncodedAsARange() throws IOException {
        List<InvokeInstruction> invokes = decoded(decodedInstructions(example("""
                .method public static highRange (I)V {
                    registers: 17,
                    parameters: { value },
                    code: {
                        invoke-static { v16 } demo/Target.one (I)V
                        return-void
                    }
                }
                """), "highRange", "(I)V"), InvokeInstruction.class);

        assertEquals(1, invokes.size());
        assertTrue(invokes.getFirst().isRange());
        assertEquals(16, invokes.getFirst().first());
        assertEquals(16, invokes.getFirst().last());
    }

    @Test
    void aRangeOfTwoHundredFiftyFiveWordsIsAccepted() throws IOException {
        List<InvokeInstruction> invokes = decoded(decodedInstructions(example("""
                .method public static widest ()V {
                    registers: 256,
                    code: {
                        invoke-static { words: { %s } } demo/Target.many ()V
                        return-void
                    }
                }
                """.formatted(consecutiveWords(255))), "widest", "()V"), InvokeInstruction.class);

        assertEquals(1, invokes.size());
        assertTrue(invokes.getFirst().isRange());
        assertEquals(0, invokes.getFirst().first());
        assertEquals(254, invokes.getFirst().last());
    }

    @Test
    void rangeFilledArrayKeepsHighRegisters() throws IOException {
        List<FilledNewArrayInstruction> arrays = decoded(decodedInstructions(example("""
                .method public static arrays (II)[I {
                    registers: 18,
                    parameters: { a, b },
                    code: {
                        filled-new-array/range { v16, v17 } [I
                        move-result-object v0
                        return-object v0
                    }
                }
                """), "arrays", "(II)[I"), FilledNewArrayInstruction.class);

        assertEquals(1, arrays.size());
        assertTrue(arrays.getFirst().isRange());
        assertEquals(16, arrays.getFirst().first());
        assertEquals(17, arrays.getFirst().last());
    }

    @Test
    void nonRangeFilledArrayKeepsTheFiveElementBoundary() throws IOException {
        List<FilledNewArrayInstruction> arrays = decoded(decodedInstructions(example("""
                .method public static fiveElements (IIIII)[I {
                    registers: 5,
                    parameters: { a, b, c, d, e },
                    code: {
                        filled-new-array { v0, v1, v2, v3, v4 } [I
                        move-result-object v0
                        return-object v0
                    }
                }
                """), "fiveElements", "(IIIII)[I"), FilledNewArrayInstruction.class);

        assertEquals(1, arrays.size());
        assertFalse(arrays.getFirst().isRange());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, arrays.getFirst().registers());
    }

    @Test
    void constDestinationAboveV255IsRejectedWithoutOutput() {
        var rejected = compileRejected(example("""
                .method public static tooHigh ()V {
                    registers: 258,
                    code: {
                        const v256 1
                        const-wide v256 1L
                        return-void
                    }
                }
                """));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(2, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        for (Diagnostic limit : limits)
            assertTrue(limit.message().contains("v256"), limit.message());
        assertTrue(errorsWithCode(rejected, DiagnosticCode.REGISTER_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(rejected.errors()));
    }

    @Test
    void literalNeedingTheSixteenBitFormRejectsRegistersAboveV15() {
        // The literal fits no eight-bit form and the sixteen-bit form cannot name v16 or v17, so one diagnostic names
        // the literal and both registers instead of reporting each register as though it alone were the problem.
        var rejected = compileRejected(example("""
                .method public static wide ()V {
                    registers: 18,
                    code: {
                        add-int v16 v17 300
                        return-void
                    }
                }
                """));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(1, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(limits.getFirst().message().contains("literal 300"), limits.getFirst().message());
        assertTrue(limits.getFirst().message().contains("v16 and v17"), limits.getFirst().message());
    }

    @Test
    void literalRejectsADestinationAboveV255() {
        var rejected = compileRejected(example("""
                .method public static wide ()V {
                    registers: 257,
                    code: {
                        add-int v256 v255 1
                        return-void
                    }
                }
                """));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(1, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(limits.getFirst().message().contains("v256"), limits.getFirst().message());
    }

    @Test
    void inPlaceArithmeticAboveV15UsesTheOrdinaryForm() throws IOException {
        // The source register v16 is above the two-address field, so the same in-place operation takes the ordinary
        // form, which keeps the destination and both sources at their written positions.
        List<BinaryInstruction> operations = decoded(decodedInstructions(example("""
                .method public static ordinary (II)V {
                    registers: 17,
                    parameters: { a, b },
                    code: {
                        add-int v15 v15 v16
                        return-void
                    }
                }
                """), "ordinary", "(II)V"), BinaryInstruction.class);

        assertEquals(1, operations.size());
        assertEquals(Opcodes.ADD_INT, operations.getFirst().opcode());
        assertEquals(15, operations.getFirst().dest());
        assertEquals(15, operations.getFirst().a());
        assertEquals(16, operations.getFirst().b());
    }

    @Test
    void threeRegisterArithmeticRejectsARegisterAboveV255() {
        var rejected = compileRejected(example("""
                .method public static threeRegister (II)V {
                    registers: 257,
                    parameters: { a, b },
                    code: {
                        add-int v0 v255 v256
                        return-void
                    }
                }
                """));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(1, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(limits.getFirst().message().contains("v256"), limits.getFirst().message());
    }

    @Test
    void listsWhoseExpandedWordsHaveNoEncodingAreRejectedWithoutOutput() {
        // A high register makes its list need a range, and a range needs consecutive words. Neither list is at most five
        // low registers, and neither is one consecutive run in the order written.
        var mixedHigh = compileRejected(example("""
                .method public static mixed (I)V {
                    registers: 17,
                    parameters: { value },
                    code: {
                        const v0 7
                        invoke-static { v16, v0 } demo/Target.two (II)V
                        return-void
                    }
                }
                """));
        var polymorphicHigh = compileRejected(example("""
                .method public static poly (I)V {
                    registers: 17,
                    parameters: { value },
                    code: {
                        const v0 7
                        invoke-polymorphic { v16, v0 } java/lang/invoke/MethodHandle.invokeExact (I)V (I)V
                        return-void
                    }
                }
                """));

        for (var rejected : List.of(mixedHigh, polymorphicHigh)) {
            List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
            assertEquals(1, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
            assertTrue(limits.getFirst().message().contains("v16"), limits.getFirst().message());
        }
    }

    @Test
    void aSixEntryListWithGapsNeedsPackingAndIsRejectedWithoutOutput() {
        // Six registers do not fit the non-range form, and they are not one run, so no encoding names them.
        var rejected = compileRejected(example("""
                .method public static gaps ()V {
                    registers: 11,
                    code: {
                        invoke-static { v0, v2, v4, v6, v8, v10 } demo/Target.six (IIIIII)V
                        return-void
                    }
                }
                """));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(1, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(limits.getFirst().message().contains("cannot be encoded"), limits.getFirst().message());
    }

    @Test
    void nonRangeFilledArrayRejectsEveryHighElement() {
        var rejected = compileRejected(example("""
                .method public static arrays (II)[I {
                    registers: 18,
                    parameters: { a, b },
                    code: {
                        filled-new-array { v16, v17 } [I
                        move-result-object v0
                        return-object v0
                    }
                }
                """));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(2, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
    }

    @Test
    void rangeCountAboveTwoHundredFiftyFiveIsRejected() {
        // A call with no parameters names its registers as raw words, so the count limit is reached with 256 words.
        var rejected = compileRejected(example("""
                .method public static tooWide ()V {
                    registers: 256,
                    code: {
                        invoke-static { words: { %s } } demo/Target.many ()V
                        return-void
                    }
                }
                """.formatted(consecutiveWords(256))));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(1, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(limits.getFirst().message().contains("256"), limits.getFirst().message());
    }

    @Test
    void rangeRunPastTheSixteenBitRegisterSpaceIsRejected() {
        var rejected = compileRejected(example("""
                .method public static past ()V {
                    code: {
                        invoke-static { words: { v65535, v65536 } } demo/Target.run ()V
                        return-void
                    }
                }
                """));

        List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
        assertEquals(1, limits.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(limits.getFirst().message().contains("v65536"), limits.getFirst().message());
    }

    @Test
    void rangeRunOutsideTheDeclaredFileIsADeclaredCountProblem() {
        var rejected = compileRejected(example("""
                .method public static run ()V {
                    registers: 4,
                    code: {
                        invoke-static { v0, v1, v2, v3, v4, v5, v6, v7 } demo/Target.run (IIIIIIII)V
                        return-void
                    }
                }
                """));

        assertFalse(errorsWithCode(rejected, DiagnosticCode.REGISTER_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(rejected.errors()));
    }

    @Test
    void declaredCountAndEncodingLimitsAreReportedSeparately() {
        var beyondDeclaredFile = compileRejected(example("""
                .method public static declared ()V {
                    registers: 8,
                    code: {
                        add-int v9 v1 300
                        return-void
                    }
                }
                """));
        var beyondEncoding = compileRejected(example("""
                .method public static encoded ()V {
                    registers: 20,
                    code: {
                        add-int v16 v1 300
                        return-void
                    }
                }
                """));

        assertFalse(errorsWithCode(beyondDeclaredFile, DiagnosticCode.REGISTER_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(beyondDeclaredFile.errors()));
        assertTrue(errorsWithCode(beyondDeclaredFile, DiagnosticCode.TARGET_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(beyondDeclaredFile.errors()));
        assertTrue(errorsWithCode(beyondEncoding, DiagnosticCode.REGISTER_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(beyondEncoding.errors()));
        assertEquals(1, errorsWithCode(beyondEncoding, DiagnosticCode.TARGET_LIMIT).size(),
                DiagnosticAssertions.formatErrors(beyondEncoding.errors()));
    }

    @Test
    void literalWithNoSingleEncodingIsRejectedForEachFormOfTheLimit() {
        // A literal beyond the sixteen-bit range, a shift literal beyond the eight-bit range, and a reverse subtraction
        // whose sixteen-bit form cannot name v200 each have no single instruction, so each reports one encoding limit.
        for (String operation : List.of("add-int v0 v1 32768", "shl-int v0 v1 300", "rsub-int v200 v100 300")) {
            var rejected = compileRejected(example("""
                    .method public static limit ()V {
                        registers: 256,
                        code: {
                            %s
                            return-void
                        }
                    }
                    """.formatted(operation)));

            List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
            assertEquals(1, limits.size(), operation + "\n" + DiagnosticAssertions.formatErrors(rejected.errors()));
        }
    }

    @Test
    void reversedNamedFilledArrayRangeIsRejected() {
        var rejected = compileRejected(example("""
                .method public static reversed (II)V {
                    registers: 4,
                    parameters: { a, b },
                    code: {
                        filled-new-array/range { b, a } [I
                        return-void
                    }
                }
                """));

        List<Diagnostic> shape = errorsWithCode(rejected, DiagnosticCode.OPERAND_SHAPE);
        assertEquals(1, shape.size(), DiagnosticAssertions.formatErrors(rejected.errors()));
        assertTrue(shape.getFirst().message().contains("reversed"), shape.getFirst().message());
    }

    private static String example(String methods) {
        return ".super java/lang/Object\n.class public Example {\n" + methods + "}\n";
    }

    /**
     * @return {@code count} register names with consecutive indices from {@code v0}, spelled for a raw word list.
     */
    private static String consecutiveWords(int count) {
        return IntStream.range(0, count).mapToObj(index -> "v" + index).collect(Collectors.joining(", "));
    }

    private static List<Instruction> decodedInstructions(String source, String name, String descriptor)
            throws IOException {
        AtomicReference<ClassDefinition> compiled = new AtomicReference<>();
        TestUtils.processDalvik(source, TestUtils.options(), result ->
                compiled.set(((DalvikClassRepresentation) result.representation()).definition()));
        byte[] bytes = DalvikDexIO.write(new DexFile(35, List.of(compiled.get())));
        ClassDefinition reread = DalvikDexIO.read(bytes).definitions().getFirst();
        return reread.getMethod(name, descriptor).getCode().getInstructions();
    }

    private static <T extends Instruction> List<T> decoded(List<Instruction> instructions, Class<T> type) {
        return instructions.stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static void assertConst(ConstInstruction instruction, int register, int value, int opcode) {
        assertEquals(opcode, instruction.opcode(), "opcode of const v" + register);
        assertEquals(register, instruction.register(), "destination of const");
        assertEquals(value, instruction.value(), "value of const v" + register);
    }

    private static void assertLiteral(BinaryLiteralInstruction instruction, int opcode, int dest, int src,
                                      int constant) {
        assertEquals(opcode, instruction.opcode(), "opcode of literal operation");
        assertEquals(dest, instruction.dest(), "destination of literal operation");
        assertEquals(src, instruction.src(), "source of literal operation");
        assertEquals(constant, instruction.constant(), "literal of literal operation");
    }

    /**
     * Compiles a source that the backend must reject. The outcome must carry errors and must not yield a class.
     */
    private static Outcome<DalvikCompileResult> compileRejected(String source) {
        Outcome<List<ASTElement>> ast =
                AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE);
        var unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(ast.requireValue(), DalvikTargetContext.INSTANCE),
                "Failed to process Dalvik source");
        Outcome<DalvikCompileResult> result = new DalvikCompiler().compile(unit, TestUtils.options());
        assertTrue(result.hasErrors(), "The encoding should be rejected");
        assertNull(result.requireValue().representation(), "A rejected compile must not yield a class");
        return result;
    }

    private static List<Diagnostic> errorsWithCode(Outcome<?> outcome, DiagnosticCode code) {
        return outcome.errors().stream().filter(diagnostic -> diagnostic.code() == code).toList();
    }
}
