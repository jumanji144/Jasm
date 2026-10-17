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
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
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
 * Verifies the raw register words of canonical invokes. Each accepted spelling is observed after a DEX write and read,
 * so the decoded words, descriptors, and printed form are what is checked. Words that no single encoding names are
 * rejected with a diagnostic and no output. The method parameters are initialized, so the checks concern the words
 * themselves rather than unrelated verification.
 */
class DalvikRawInvokeTest {

    @Test
    void repeatedWordsWithinTheNonRangeLimitKeepTheirOrderAndCount() throws IOException {
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static repeat (II)V {
                    registers: 2,
                    parameters: { a, b },
                    code: {
                        invoke-static { words: { v1, v0, v1, v0, v1 } } demo/Target.five (IIIII)V
                        return-void
                    }
                }
                """), "repeat", "(II)V")).getFirst();

        assertFalse(invoke.isRange());
        assertArrayEquals(new int[] {1, 0, 1, 0, 1}, invoke.arguments());
    }

    @Test
    void sixConsecutiveWordsNameARangeFromTheirFirstRegister() throws IOException {
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static six (IIIIII)V {
                    registers: 6,
                    parameters: { a, b, c, d, e, f },
                    code: {
                        invoke-static { words: { v0, v1, v2, v3, v4, v5 } } demo/Target.six (IIIIII)V
                        return-void
                    }
                }
                """), "six", "(IIIIII)V")).getFirst();

        assertTrue(invoke.isRange());
        assertEquals(0, invoke.first());
        assertEquals(5, invoke.last());
        assertEquals("demo/Target", invoke.owner().internalName());
        assertEquals("six", invoke.name());
        assertEquals("(IIIIII)V", invoke.methodType().descriptor());
    }

    @Test
    void aSingleHighWordNamesARangeBecauseTheNonRangeFieldCannotHoldIt() throws IOException {
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static high (I)V {
                    registers: 17,
                    parameters: { value },
                    code: {
                        invoke-static { words: { v16 } } demo/Target.one (I)V
                        return-void
                    }
                }
                """), "high", "(I)V")).getFirst();

        assertTrue(invoke.isRange());
        assertEquals(16, invoke.first());
        assertEquals(16, invoke.last());
    }

    @Test
    void aConsecutiveHighPairNamesARangeWithItsWideArgumentIntact() throws IOException {
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static highPair (J)V {
                    registers: 18,
                    parameters: { wide },
                    code: {
                        invoke-static { words: { v16, v17 } } demo/Target.pair (J)V
                        return-void
                    }
                }
                """), "highPair", "(J)V")).getFirst();

        assertTrue(invoke.isRange());
        assertEquals(16, invoke.first());
        assertEquals(17, invoke.last());
    }

    @Test
    void anEmptyWordListStaysANonRangeInvokeWithNoWords() throws IOException {
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static none ()V {
                    code: {
                        invoke-static { words: { } } demo/Target.none ()V
                        return-void
                    }
                }
                """), "none", "()V")).getFirst();

        assertFalse(invoke.isRange());
        assertEquals(0, invoke.arguments().length);
    }

    @Test
    void aMalformedWidePairKeepsItsWordsAndPrintsRawThenReassemblesToThem() throws IOException {
        ClassDefinition decoded = dexRoundTrip(compile(example("""
                .method public static pair (JI)V {
                    registers: 3,
                    parameters: { wide, count },
                    code: {
                        invoke-static { words: { v0, v2 } } demo/Target.call (J)V
                        return-void
                    }
                }
                """)));
        assertArrayEquals(new int[] {0, 2}, invokes(instructions(decoded, "pair", "(JI)V")).getFirst().arguments());

        String printed = printClass(decoded);
        assertTrue(printed.contains("words:"), printed);

        ClassDefinition reassembled = dexRoundTrip(compile(printed));
        assertArrayEquals(new int[] {0, 2}, invokes(instructions(reassembled, "pair", "(JI)V")).getFirst().arguments());
    }

    @Test
    void anArgumentCountMismatchPrintsRawAndReassemblesToTheSameWords() throws IOException {
        ClassDefinition decoded = dexRoundTrip(compile(example("""
                .method public static count (JI)V {
                    registers: 3,
                    parameters: { wide, count },
                    code: {
                        invoke-static { words: { v0, v1, v2 } } demo/Target.call (J)V
                        return-void
                    }
                }
                """)));

        String printed = printClass(decoded);
        assertTrue(printed.contains("words:"), printed);

        ClassDefinition reassembled = dexRoundTrip(compile(printed));
        assertArrayEquals(new int[] {0, 1, 2}, invokes(instructions(reassembled, "count", "(JI)V")).getFirst().arguments());
    }

    @Test
    void aFaithfulWideListKeepsItsOrdinarySpellingWhoseValuesAreUnknown() throws IOException {
        ClassDefinition decoded = dexRoundTrip(compile(example("""
                .method public static wide (JI)V {
                    registers: 3,
                    parameters: { wide, count },
                    code: {
                        invoke-static { v0, v1 } demo/Target.call (J)V
                        return-void
                    }
                }
                """)));

        String printed = printClass(decoded);
        assertFalse(printed.contains("words:"), printed);

        ClassDefinition reassembled = dexRoundTrip(compile(printed));
        assertArrayEquals(new int[] {0, 1}, invokes(instructions(reassembled, "wide", "(JI)V")).getFirst().arguments());
    }

    @Test
    void customInvokeRawRangeKeepsItsCallSiteAndBootstrapThroughPrinting() throws IOException {
        ClassDefinition decoded = dexRoundTrip(compile(example("""
                .method public static custom (JI)V {
                    registers: 19,
                    parameters: { wide, count },
                    code: {
                        invoke-custom { words: { v16, v17, v18 } } callsite (JI)V ConstantBootstraps.nullConstant { "demo", 7 }
                        return-void
                    }
                }
                """)));
        InvokeCustomInstruction original = customs(instructions(decoded, "custom", "(JI)V")).getFirst();

        assertTrue(original.isRange());
        assertEquals(16, original.first());
        assertEquals(18, original.last());
        assertEquals("callsite", original.name());
        assertEquals("(JI)V", original.type().descriptor());

        ClassDefinition reassembled = dexRoundTrip(compile(printClass(decoded)));
        InvokeCustomInstruction reread = customs(instructions(reassembled, "custom", "(JI)V")).getFirst();
        assertEquals(original.first(), reread.first());
        assertEquals(original.last(), reread.last());
        assertEquals(original.name(), reread.name());
        assertEquals(original.type().descriptor(), reread.type().descriptor());
        assertEquals(original.handle(), reread.handle());
        assertEquals(original.arguments(), reread.arguments());
    }

    @Test
    void polymorphicRawRangeKeepsItsDeclaredAndCallSitePrototypesThroughPrinting() throws IOException {
        ClassDefinition decoded = dexRoundTrip(compile(example("""
                .method public static poly (Ljava/lang/Object;JJI)V {
                    registers: 6,
                    parameters: { receiver, first, second, count },
                    code: {
                        invoke-polymorphic { words: { v0, v1, v2, v3, v4, v5 } } java/lang/invoke/MethodHandle.invoke ([Ljava/lang/Object;)V (JJI)V
                        return-void
                    }
                }
                """)));
        InvokeInstruction original = invokes(instructions(decoded, "poly", "(Ljava/lang/Object;JJI)V")).getFirst();

        assertEquals(Opcodes.INVOKE_POLYMORPHIC, original.opcode());
        assertTrue(original.isRange());
        assertEquals(0, original.first());
        assertEquals(5, original.last());
        assertEquals("([Ljava/lang/Object;)V", original.methodType().descriptor());
        assertEquals("(JJI)V", original.type().descriptor());

        ClassDefinition reassembled = dexRoundTrip(compile(printClass(decoded)));
        InvokeInstruction reread = invokes(instructions(reassembled, "poly", "(Ljava/lang/Object;JJI)V")).getFirst();
        assertEquals(original.first(), reread.first());
        assertEquals(original.last(), reread.last());
        assertEquals(original.methodType().descriptor(), reread.methodType().descriptor());
        assertEquals(original.type().descriptor(), reread.type().descriptor());
    }

    @Test
    void rawWordsReserveTheirSlotsSoNamedLocalsAvoidThem() throws IOException {
        List<ConstInstruction> consts = decoded(decodedInstructions(example("""
                .method public static reserve ()V {
                    registers: 4,
                    code: {
                        const local 1
                        invoke-static { words: { v0, v1 } } demo/Target.call (II)V
                        return-void
                    }
                }
                """), "reserve", "()V"), ConstInstruction.class);

        assertEquals(1, consts.size());
        assertEquals(2, consts.getFirst().register());
    }

    @Test
    void theRangeCountBoundaryIsTwoHundredFiftyFiveWords() throws IOException {
        InvokeInstruction accepted = invokes(decodedInstructions(example("""
                .method public static many ()V {
                    registers: 255,
                    code: {
                        invoke-static { words: { %s } } demo/Target.many ()V
                        return-void
                    }
                }
                """.formatted(consecutiveWords(255))), "many", "()V")).getFirst();

        assertTrue(accepted.isRange());
        assertEquals(0, accepted.first());
        assertEquals(254, accepted.last());

        var rejected = compileRejected(example("""
                .method public static tooMany ()V {
                    registers: 256,
                    code: {
                        invoke-static { words: { %s } } demo/Target.many ()V
                        return-void
                    }
                }
                """.formatted(consecutiveWords(256))));
        assertEquals(1, errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT).size(),
                DiagnosticAssertions.formatErrors(rejected.errors()));
    }

    @Test
    void wordsWithNoSingleEncodingAreRejectedWithoutOutput() {
        // Each list is neither at most five registers within v0 through v15 nor a consecutive run in the written order.
        for (String words : List.of("v0, v16", "v17, v16", "v0, v1, v2, v3, v4, v9")) {
            var rejected = compileRejected(example("""
                    .method public static gap ()V {
                        registers: 18,
                        code: {
                            invoke-static { words: { %s } } demo/Target.gap ()V
                            return-void
                        }
                    }
                    """.formatted(words)));

            List<Diagnostic> limits = errorsWithCode(rejected, DiagnosticCode.TARGET_LIMIT);
            assertEquals(1, limits.size(), words + "\n" + DiagnosticAssertions.formatErrors(rejected.errors()));
            assertTrue(limits.getFirst().message().contains("cannot be encoded"), limits.getFirst().message());
        }
    }

    @Test
    void aRawWordBeyondTheDeclaredRegisterCountIsReportedAsTheCountLimit() {
        var rejected = compileRejected(example("""
                .method public static beyond ()V {
                    registers: 4,
                    code: {
                        invoke-static { words: { v5 } } demo/Target.one ()V
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
    void rawWordsAreAcceptedOnlyByCanonicalInvokesAndWellFormedObjects() {
        for (String instruction : List.of(
                "invoke-static/range { words: { v0, v1 } } demo/Target.call (II)V",
                "filled-new-array { words: { v0, v1 } } [I",
                "invoke-static { words: { v0, 1 } } demo/Target.call (II)V",
                "invoke-static { words: { v0 }, extra: { v1 } } demo/Target.call (II)V")) {
            List<Diagnostic> errors = rejectionErrors(example("""
                    .method public static raw (II)V {
                        registers: 2,
                        parameters: { a, b },
                        code: {
                            %s
                            return-void
                        }
                    }
                    """.formatted(instruction)));

            assertFalse(errors.isEmpty(), instruction);
        }
    }

    @Test
    void theReceiverAndTheWideTailAreCountedAtThePrintedBoundary() throws IOException {
        // An instance invoke leads with its receiver, so (J)V takes three words there and two for a static call. Only the
        // exact count prints in the ordinary spelling, and the count is judged from the decoded words, not the source.
        assertFalse(printedCall("invoke-virtual { v0, v1, v2 } demo/Target.call (J)V").contains("words:"));
        assertTrue(printedCall("invoke-virtual { v0, v1 } demo/Target.call (J)V").contains("words:"));
        assertTrue(printedCall("invoke-static { v0 } demo/Target.call (J)V").contains("words:"));
    }

    @Test
    void aPolymorphicRawListKeepsItsWordsAndBothPrototypes() throws IOException {
        InvokeInstruction invoke = invokes(decodedInstructions(example("""
                .method public static listed (Ljava/lang/Object;I)V {
                    registers: 2,
                    parameters: { receiver, count },
                    code: {
                        invoke-polymorphic { words: { v1, v0 } } java/lang/invoke/MethodHandle.invokeExact ()V (I)V
                        return-void
                    }
                }
                """), "listed", "(Ljava/lang/Object;I)V")).getFirst();

        assertFalse(invoke.isRange());
        assertArrayEquals(new int[] {1, 0}, invoke.arguments());
        assertEquals("()V", invoke.methodType().descriptor());
        assertEquals("(I)V", invoke.type().descriptor());
    }

    @Test
    void aCustomRawListKeepsItsWordOrderWithoutSorting() throws IOException {
        InvokeCustomInstruction custom = customs(decodedInstructions(example("""
                .method public static listed (JI)V {
                    registers: 3,
                    parameters: { wide, count },
                    code: {
                        invoke-custom { words: { v2, v0, v1 } } callsite (JI)V ConstantBootstraps.nullConstant { "demo" }
                        return-void
                    }
                }
                """), "listed", "(JI)V")).getFirst();

        assertFalse(custom.isRange());
        assertArrayEquals(new int[] {2, 0, 1}, custom.argumentRegisters());
    }

    @Test
    void rawWordsOnAFormThatDoesNotAcceptThemNameTheRestriction() {
        for (String instruction : List.of(
                "invoke-static/range { words: { v0, v1 } } demo/Target.call (II)V",
                "filled-new-array { words: { v0, v1 } } [I")) {
            List<Diagnostic> errors = rejectionErrors(example("""
                    .method public static raw (II)V {
                        registers: 2,
                        parameters: { a, b },
                        code: {
                            %s
                            return-void
                        }
                    }
                    """.formatted(instruction)));

            assertTrue(errors.stream().anyMatch(diagnostic -> diagnostic.message().contains("canonical invoke mnemonics")),
                    instruction + "\n" + DiagnosticAssertions.formatErrors(errors));
        }
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

    /**
     * @return The printed form of a class whose only method makes {@code call} with the parameters {@code (JI)V}, after a
     * DEX write and read.
     */
    private static String printedCall(String call) throws IOException {
        return printClass(dexRoundTrip(compile(example("""
                .method public static boundary (JI)V {
                    registers: 3,
                    parameters: { wide, count },
                    code: {
                        %s
                        return-void
                    }
                }
                """.formatted(call)))));
    }

    private static <T extends Instruction> List<T> decoded(List<Instruction> instructions, Class<T> type) {
        return instructions.stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static List<InvokeInstruction> invokes(List<Instruction> instructions) {
        return decoded(instructions, InvokeInstruction.class);
    }

    private static List<InvokeCustomInstruction> customs(List<Instruction> instructions) {
        return decoded(instructions, InvokeCustomInstruction.class);
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

    /**
     * Processes a source that must not be accepted, which may fail in semantic processing or in the backend. The source
     * must parse, and neither stage may yield a class.
     *
     * @return The errors that rejected the source.
     */
    private static List<Diagnostic> rejectionErrors(String source) {
        Outcome<List<ASTElement>> ast =
                AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE);
        var processed = SemanticProcessor.process(ast.requireValue(), DalvikTargetContext.INSTANCE);
        if (processed.hasErrors())
            return processed.errors();

        Outcome<DalvikCompileResult> result = new DalvikCompiler().compile(processed.requireValue(), TestUtils.options());
        assertTrue(result.hasErrors(), "The source should be rejected");
        assertNull(result.requireValue().representation(), "A rejected compile must not yield a class");
        return result.errors();
    }

    private static List<Diagnostic> errorsWithCode(Outcome<?> outcome, DiagnosticCode code) {
        return outcome.errors().stream().filter(diagnostic -> diagnostic.code() == code).toList();
    }
}
