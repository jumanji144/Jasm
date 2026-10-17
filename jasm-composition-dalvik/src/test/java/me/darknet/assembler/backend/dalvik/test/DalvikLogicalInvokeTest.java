package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies invokes written as logical arguments. The descriptor decides how many entries a list has, a long or double
 * entry names its first register and its tail is implicit, a receiver is the first entry of an instance invoke, and the
 * encoding is chosen from the words once they are expanded. Accepted calls are observed after a DEX write and read, so
 * the decoded words are what is checked.
 */
class DalvikLogicalInvokeTest {

    @Test
    void aWideEntryNamesItsFirstRegisterAndItsTailIsEncodedAfterIt() throws IOException {
        // (JI)V takes two entries, so v0 names the long, whose tail is v1, and v2 names the int.
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static wide (JI)V {
                    registers: 3,
                    parameters: { wide, count },
                    code: {
                        invoke-static { v0, v2 } demo/Target.call (JI)V
                        return-void
                    }
                }
                """), "wide", "(JI)V")).getFirst();

        assertFalse(invoke.isRange());
        assertArrayEquals(new int[] {0, 1, 2}, invoke.arguments());
    }

    @Test
    void theDescriptorDecidesTheEntryCountNotTheWordCount() {
        // (J)V takes one entry for its long, so a second entry is an error even though the long spans two words.
        List<Diagnostic> errors = semanticErrors(example("""
                .method public static count (J)V {
                    registers: 2,
                    parameters: { wide },
                    code: {
                        invoke-static { v0, v1 } demo/Target.call (J)V
                        return-void
                    }
                }
                """));

        assertTrue(errors.stream().anyMatch(diagnostic -> diagnostic.message().contains("takes 1 argument for (J)V")),
                DiagnosticAssertions.formatErrors(errors));
    }

    @Test
    void aReceiverIsTheFirstEntryOfAnInstanceInvoke() throws IOException {
        // The receiver is one entry and the int argument is the next, so the call names v0 and v1.
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static receiver (Ljava/lang/Object;I)V {
                    registers: 2,
                    parameters: { target, count },
                    code: {
                        invoke-virtual { target, count } demo/Target.call (I)V
                        return-void
                    }
                }
                """), "receiver", "(Ljava/lang/Object;I)V")).getFirst();

        assertEquals(Opcodes.INVOKE_VIRTUAL, invoke.opcode());
        assertFalse(invoke.isRange());
        assertArrayEquals(new int[] {0, 1}, invoke.arguments());
    }

    @Test
    void anEmptyInstanceListIsAnErrorRatherThanARawFallback() {
        // An instance invoke must name its receiver, and only the words key writes registers without the descriptor.
        List<Diagnostic> errors = semanticErrors(example("""
                .method public static empty ()V {
                    code: {
                        invoke-virtual { } demo/Target.call ()V
                        return-void
                    }
                }
                """));

        assertTrue(errors.stream().anyMatch(diagnostic -> diagnostic.message().contains("takes a receiver and 0 arguments")),
                DiagnosticAssertions.formatErrors(errors));
    }

    @Test
    void anEmptyStaticListIsANonRangeInvokeWithNoWords() throws IOException {
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static none ()V {
                    code: {
                        invoke-static { } demo/Target.none ()V
                        return-void
                    }
                }
                """), "none", "()V")).getFirst();

        assertFalse(invoke.isRange());
        assertEquals(0, invoke.arguments().length);
    }

    @Test
    void aHighWideEntryIsEncodedAsARangeOfItsTwoWords() throws IOException {
        // v16 does not fit a non-range field, so the consecutive pair v16 and v17 is named by a range.
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static highWide (J)V {
                    registers: 18,
                    parameters: { wide },
                    code: {
                        invoke-static { wide } demo/Target.call (J)V
                        return-void
                    }
                }
                """), "highWide", "(J)V")).getFirst();

        assertTrue(invoke.isRange());
        assertEquals(16, invoke.first());
        assertEquals(17, invoke.last());
    }

    @Test
    void aSixEntryListOfConsecutiveWordsIsEncodedAsARange() throws IOException {
        // Six words are more than the non-range form names, and they are consecutive, so the range form carries them.
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static six (IIIIII)V {
                    registers: 6,
                    parameters: { a, b, c, d, e, f },
                    code: {
                        invoke-static { v0, v1, v2, v3, v4, v5 } demo/Target.six (IIIIII)V
                        return-void
                    }
                }
                """), "six", "(IIIIII)V")).getFirst();

        assertTrue(invoke.isRange());
        assertEquals(0, invoke.first());
        assertEquals(5, invoke.last());
    }

    @Test
    void aListOfFewerThanSixEntriesThatExpandsPastFiveWordsIsEncodedAsARange() throws IOException {
        // Three long arguments are three entries and six words, which the non-range form cannot name, so they form a range.
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static longs (JJJ)V {
                    registers: 6,
                    parameters: { a, b, c },
                    code: {
                        invoke-static { v0, v2, v4 } demo/Target.longs (JJJ)V
                        return-void
                    }
                }
                """), "longs", "(JJJ)V")).getFirst();

        assertTrue(invoke.isRange());
        assertEquals(0, invoke.first());
        assertEquals(5, invoke.last());
    }

    @Test
    void aLowNoncontiguousListFitsTheNonRangeFormInItsWrittenOrder() throws IOException {
        // Four low registers in any order fit the non-range form, which names them in the order they were written.
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static gaps (IIIII)V {
                    registers: 5,
                    parameters: { a, b, c, d, e },
                    code: {
                        invoke-static { v3, v0, v4, v2 } demo/Target.four (IIII)V
                        return-void
                    }
                }
                """), "gaps", "(IIIII)V")).getFirst();

        assertFalse(invoke.isRange());
        assertArrayEquals(new int[] {3, 0, 4, 2}, invoke.arguments());
    }

    @Test
    void aNamedWideValueTakesAFreePairBesideAnExplicitRegister() throws IOException {
        // A first-fit slot would be v0, whose high word v1 is an explicit register. The pair has to be v2 and v3.
        List<Instruction> instructions = decodedInstructions(example("""
                .method public static pair (I)V {
                    registers: 5,
                    parameters: { value },
                    code: {
                        const v1 9
                        const-wide wide 7
                        invoke-static { wide } demo/Target.take (J)V
                        return-void
                    }
                }
                """), "pair", "(I)V");

        assertEquals(1, decoded(instructions, ConstInstruction.class).getFirst().register());
        assertEquals(2, decoded(instructions, ConstWideInstruction.class).getFirst().register());
        assertArrayEquals(new int[] {2, 3}, invokes(instructions).getFirst().arguments());
    }

    @Test
    void aNumericWideEntryReservesItsTailAgainstANewLocal() throws IOException {
        // v0 is wide, so v1 is its tail and cannot hold the new local, which takes v2.
        List<Instruction> instructions = decodedInstructions(example("""
                .method public static tail ()V {
                    registers: 4,
                    code: {
                        const-wide v0 5
                        invoke-static { v0 } demo/Target.take (J)V
                        const local 3
                        return-void
                    }
                }
                """), "tail", "()V");

        assertEquals(2, decoded(instructions, ConstInstruction.class).getFirst().register());
        assertArrayEquals(new int[] {0, 1}, invokes(instructions).getFirst().arguments());
    }

    @Test
    void aWideEntryWhoseTailLiesBeyondTheDeclaredFileIsRejected() {
        // A numeric high word past the declared count is a count problem. A named pair needs two registers below the
        // declared count, and a one-register file has no room for one.
        var numeric = compileRejected(example("""
                .method public static tail ()V {
                    registers: 2,
                    code: {
                        invoke-static { v1 } demo/Target.take (J)V
                        return-void
                    }
                }
                """));
        var named = compileRejected(example("""
                .method public static pair ()V {
                    registers: 1,
                    code: {
                        invoke-static { wide } demo/Target.take (J)V
                        return-void
                    }
                }
                """));

        assertFalse(errorsWithCode(numeric, DiagnosticCode.REGISTER_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(numeric.errors()));
        assertFalse(errorsWithCode(named, DiagnosticCode.REGISTER_LIMIT).isEmpty(),
                DiagnosticAssertions.formatErrors(named.errors()));
    }

    @Test
    void aWideParameterKeepsItsBindingUnderEachAliasWithoutNewSlots() throws IOException {
        // The parameter's name and its pN alias name the same pair, so both calls name v0 and v1 and neither needs a slot.
        List<InvokeInstruction> invokes = invokes(decodedInstructions(example("""
                .method public static alias (J)V {
                    registers: 2,
                    parameters: { wide },
                    code: {
                        invoke-static { wide } demo/Target.take (J)V
                        invoke-static { p0 } demo/Target.take (J)V
                        return-void
                    }
                }
                """), "alias", "(J)V"));

        assertEquals(2, invokes.size());
        assertArrayEquals(new int[] {0, 1}, words(invokes.get(0)));
        assertArrayEquals(new int[] {0, 1}, words(invokes.get(1)));
    }

    @Test
    void mixedInstanceAndStaticCallsRoundTripThroughPrintingWithTheSameWords() throws IOException {
        // The instance call names v14 through v17, and the static call names the pair v15 and v16. Both need a range, and
        // printing lists their logical arguments, so reassembling must reproduce the same words.
        ClassDefinition definition = dexRoundTrip(compile(example("""
                .method public static mixed (Ljava/lang/Object;JI)V {
                    registers: 18,
                    parameters: { target, wide, count },
                    code: {
                        invoke-virtual { target, wide, count } demo/Target.call (JI)V
                        invoke-static { wide } demo/Target.take (J)V
                        return-void
                    }
                }
                """)));
        List<InvokeInstruction> original = invokes(instructions(definition, "mixed", "(Ljava/lang/Object;JI)V"));

        String printed = printClass(definition);
        assertFalse(printed.contains("words:"), printed);

        ClassDefinition reassembled = dexRoundTrip(compile(printed));
        List<InvokeInstruction> reread = invokes(instructions(reassembled, "mixed", "(Ljava/lang/Object;JI)V"));
        assertEquals(original.size(), reread.size(), printed);
        for (int index = 0; index < original.size(); index++) {
            assertEquals(original.get(index).opcode(), reread.get(index).opcode(), printed);
            assertEquals(original.get(index).isRange(), reread.get(index).isRange(), printed);
            assertArrayEquals(words(original.get(index)), words(reread.get(index)), printed);
        }
    }

    @Test
    void customAndPolymorphicCallsTakeLogicalArgumentsAndKeepTheirDescriptors() throws IOException {
        // The custom call takes only its call-site arguments, and the polymorphic call takes its receiver first.
        ClassDefinition definition = dexRoundTrip(compile(example("""
                .method public static calls (Ljava/lang/Object;JI)V {
                    registers: 4,
                    parameters: { receiver, wide, count },
                    code: {
                        invoke-custom { wide, count } callsite (JI)V ConstantBootstraps.nullConstant { "demo" }
                        invoke-polymorphic { receiver, wide, count } java/lang/invoke/MethodHandle.invokeExact ([Ljava/lang/Object;)V (JI)V
                        return-void
                    }
                }
                """)));
        List<Instruction> calls = instructions(definition, "calls", "(Ljava/lang/Object;JI)V");
        InvokeCustomInstruction custom = customs(calls).getFirst();
        InvokeInstruction polymorphic = polymorphics(calls).getFirst();

        assertFalse(custom.isRange());
        assertArrayEquals(new int[] {1, 2, 3}, custom.argumentRegisters());
        assertEquals("(JI)V", custom.type().descriptor());
        assertFalse(polymorphic.isRange());
        assertArrayEquals(new int[] {0, 1, 2, 3}, words(polymorphic));
        assertEquals("([Ljava/lang/Object;)V", polymorphic.methodType().descriptor());
        assertEquals("(JI)V", polymorphic.type().descriptor());

        String printed = printClass(definition);
        assertFalse(printed.contains("words:"), printed);
        ClassDefinition reassembled = dexRoundTrip(compile(printed));
        List<Instruction> rereadInstructions = instructions(reassembled, "calls", "(Ljava/lang/Object;JI)V");
        assertArrayEquals(custom.argumentRegisters(), customs(rereadInstructions).getFirst().argumentRegisters(), printed);
        assertArrayEquals(words(polymorphic), words(polymorphics(rereadInstructions).getFirst()), printed);
    }

    /**
     * @return The register words of an invoke in encoded order, expanding a range from its first to its last register.
     */
    private static int[] words(InvokeInstruction invoke) {
        if (!invoke.isRange())
            return invoke.arguments();
        int[] words = new int[invoke.last() - invoke.first() + 1];
        for (int index = 0; index < words.length; index++)
            words[index] = invoke.first() + index;
        return words;
    }

    private static String example(String methods) {
        return ".super java/lang/Object\n.class public Example {\n" + methods + "}\n";
    }

    private static ClassDefinition compile(String source) {
        AtomicReference<ClassDefinition> compiled = new AtomicReference<>();
        TestUtils.processDalvik(source, TestUtils.options(), result ->
                compiled.set(((DalvikClassRepresentation) result.representation()).definition()));
        return compiled.get();
    }

    private static ClassDefinition dexRoundTrip(ClassDefinition definition) throws IOException {
        byte[] bytes = DalvikDexIO.write(new DexFile(35, List.of(definition)));
        return DalvikDexIO.read(bytes).definitions().getFirst();
    }

    private static List<Instruction> decodedInstructions(String source, String name, String descriptor)
            throws IOException {
        return instructions(dexRoundTrip(compile(source)), name, descriptor);
    }

    private static List<Instruction> instructions(ClassDefinition definition, String name, String descriptor) {
        return definition.getMethod(name, descriptor).getCode().getInstructions();
    }

    private static String printClass(ClassDefinition definition) {
        DalvikClassPrinter printer = new DalvikClassPrinter(definition);
        PrintContext<?> context = new PrintContext<>("\t");
        printer.print(context);
        return TestUtils.normalize(context.toString());
    }

    private static <T extends Instruction> List<T> decoded(List<Instruction> instructions, Class<T> type) {
        return instructions.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /**
     * @return The ordinary invokes of a method, excluding the polymorphic and custom forms.
     */
    private static List<InvokeInstruction> invokes(List<Instruction> instructions) {
        return decoded(instructions, InvokeInstruction.class).stream()
                .filter(invoke -> invoke.opcode() != Opcodes.INVOKE_POLYMORPHIC
                        && invoke.opcode() != Opcodes.INVOKE_POLYMORPHIC_RANGE
                        && invoke.opcode() != Opcodes.INVOKE_CUSTOM
                        && invoke.opcode() != Opcodes.INVOKE_CUSTOM_RANGE)
                .toList();
    }

    private static List<InvokeInstruction> polymorphics(List<Instruction> instructions) {
        return decoded(instructions, InvokeInstruction.class).stream()
                .filter(invoke -> invoke.opcode() == Opcodes.INVOKE_POLYMORPHIC
                        || invoke.opcode() == Opcodes.INVOKE_POLYMORPHIC_RANGE)
                .toList();
    }

    private static List<InvokeCustomInstruction> customs(List<Instruction> instructions) {
        return decoded(instructions, InvokeCustomInstruction.class);
    }

    /**
     * Processes a source only as far as semantic processing, which is where the count of a logical list is judged.
     *
     * @return The errors that rejected the source, which must be present.
     */
    private static List<Diagnostic> semanticErrors(String source) {
        Outcome<List<ASTElement>> ast =
                AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE);
        var processed = SemanticProcessor.process(ast.requireValue(), DalvikTargetContext.INSTANCE);
        assertTrue(processed.hasErrors(), "The source should fail semantic processing");
        return processed.errors();
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
