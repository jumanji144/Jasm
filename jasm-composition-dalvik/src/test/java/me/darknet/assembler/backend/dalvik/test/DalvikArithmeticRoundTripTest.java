package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.instructions.Binary2AddrInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryLiteralInstruction;
import me.darknet.dex.tree.definitions.instructions.BranchInstruction;
import me.darknet.dex.tree.definitions.instructions.BranchZeroInstruction;
import me.darknet.dex.tree.definitions.instructions.GotoInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round-trips arithmetic through source, DEX, printed source, and DEX again. Each decoded method is compared by the
 * encodings its arithmetic selected, its branch targets, its line associations, and its try ranges. Targets are compared
 * by their position among the executable instructions, so a label's identity does not affect the comparison.
 */
class DalvikArithmeticRoundTripTest {
    /**
     * Covers the ordinary, two-address, eight-bit literal, sixteen-bit literal, and eight-bit reverse-subtraction
     * encodings, a branch, line directives, and a handler that covers the arithmetic.
     */
    private static final String SOURCE = """
            .super java/lang/Object
            .class public Example {
                .method public static mix (II)I {
                    registers: 6,
                    parameters: { a, b },
                    exceptions: {
                        { Start, End, Handler, java/lang/RuntimeException }
                    },
                    code: {
                        line 10
                    Start:
                        add-int v0 a b
                        line 11
                        add-int v1 v0 3
                        line 12
                        add-int v1 v1 300
                        line 13
                        rsub-int v2 v1 -7
                        line 14
                        add-int v2 v2 a
                        if-eqz v2 Zero
                        line 15
                    End:
                        return v2
                    Zero:
                        line 16
                        const v3 0
                        return v3
                    Handler:
                        line 17
                        move-exception v5
                        const v3 -1
                        return v3
                    }
                }
            }
            """;

    @Test
    void printedArithmeticRoundTripsWithBranchesLinesAndHandlers() throws IOException {
        ClassDefinition decoded = dexRoundTrip(compile(SOURCE));
        Trace original = trace(decoded, "mix", "(II)I");

        // The method must exercise each encoding it is meant to cover, so the comparison below is not vacuous.
        assertEquals(List.of(Opcodes.ADD_INT, Opcodes.ADD_INT_LIT8, Opcodes.ADD_INT_LIT16, Opcodes.RSUB_INT_LIT8,
                        Opcodes.ADD_INT_2ADDR),
                original.operations().stream().map(List::getFirst).toList(), original.toString());
        // Positions count executable instructions, so 0 is the first arithmetic instruction and 6 is the return that
        // follows the branch. Each line is associated with the instruction it introduces, the branch targets Zero, and
        // the try range covers the arithmetic and hands off to the handler.
        assertEquals(List.of(List.of(0, 10), List.of(1, 11), List.of(2, 12), List.of(3, 13), List.of(4, 14),
                List.of(6, 15), List.of(7, 16), List.of(9, 17)), original.lines(), original.toString());
        assertEquals(7, original.branches().getFirst().getLast(), original.toString());
        assertEquals(List.of("0..6 -> 9 java/lang/RuntimeException"), original.handlers(), original.toString());

        String printed = printClass(decoded);
        Trace recompiled = trace(dexRoundTrip(compile(printed)), "mix", "(II)I");

        assertEquals(original, recompiled, printed);
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

    private static String printClass(ClassDefinition definition) {
        DalvikClassPrinter printer = new DalvikClassPrinter(definition);
        PrintContext<?> context = new PrintContext<>("\t");
        printer.print(context);
        return TestUtils.normalize(context.toString());
    }

    /**
     * @return Behavior of one decoded method, keyed by the encodings and positions that the round trip must preserve.
     */
    private static Trace trace(ClassDefinition definition, String name, String descriptor) {
        Code code = definition.getMethod(name, descriptor).getCode();
        List<Instruction> instructions = code.getInstructions();
        List<List<Integer>> operations = new ArrayList<>();
        List<List<Integer>> branches = new ArrayList<>();
        List<List<Integer>> lines = new ArrayList<>();
        List<String> handlers = new ArrayList<>();

        for (Instruction instruction : instructions) {
            switch (instruction) {
                case Label label when label.lineNumber() != Label.UNASSIGNED ->
                        lines.add(List.of(position(instructions, label), label.lineNumber()));
                case BinaryInstruction binary ->
                        operations.add(List.of(binary.opcode(), binary.dest(), binary.a(), binary.b()));
                case Binary2AddrInstruction binary ->
                        operations.add(List.of(binary.opcode(), binary.a(), binary.b()));
                case BinaryLiteralInstruction literal ->
                        operations.add(List.of(literal.opcode(), literal.dest(), literal.src(), literal.constant()));
                case BranchInstruction branch ->
                        branches.add(List.of(branch.opcode(), branch.a(), branch.b(), position(instructions, branch.label())));
                case BranchZeroInstruction branch ->
                        branches.add(List.of(branch.opcode(), branch.a(), position(instructions, branch.label())));
                case GotoInstruction jump ->
                        branches.add(List.of(jump.opcode(), position(instructions, jump.jump())));
                default -> {}
            }
        }

        for (TryCatch tryCatch : code.tryCatch()) {
            for (Handler handler : tryCatch.handlers()) {
                String type = handler.exceptionType() == null ? "*" : handler.exceptionType().internalName();
                handlers.add(position(instructions, tryCatch.begin()) + ".." + position(instructions, tryCatch.end())
                        + " -> " + position(instructions, handler.handler()) + " " + type);
            }
        }
        return new Trace(operations, branches, lines, handlers);
    }

    /**
     * @return Position of {@code label} among the executable instructions, which is the index of the instruction after it.
     */
    private static int position(List<Instruction> instructions, Label label) {
        int executable = 0;
        for (Instruction instruction : instructions) {
            if (instruction == label)
                return executable;
            if (!(instruction instanceof Label))
                executable++;
        }
        throw new AssertionError("Label is not part of the method code");
    }

    /**
     * Behavior of one decoded method.
     *
     * @param operations
     * 		Opcode and register operands of each arithmetic instruction, in order.
     * @param branches
     * 		Opcode, operands and target position of each branch and goto, in order.
     * @param lines
     * 		Position and line number of each line association, in order.
     * @param handlers
     * 		Each try range with its handler position and exception type, in order.
     */
    private record Trace(List<List<Integer>> operations, List<List<Integer>> branches,
                         List<List<Integer>> lines, List<String> handlers) {}
}
