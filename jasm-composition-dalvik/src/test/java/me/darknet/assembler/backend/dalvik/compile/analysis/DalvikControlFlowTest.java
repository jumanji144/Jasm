package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.analysis.registry.BasicFieldValueLookup;
import me.darknet.assembler.analysis.registry.BasicMethodValueLookup;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.PrimitiveType;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.type.Types;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DalvikControlFlowTest {
    @Test
    void graphCollectionsAreDefensiveSnapshots() {
        Map<Integer, List<Integer>> normalInput = new LinkedHashMap<>();
        List<Integer> normalEdges = new ArrayList<>(List.of(7));
        normalInput.put(4, normalEdges);
        Label handlerLabel = new Label();
        DalvikControlFlowGraph.ExceptionEdge exceptionEdge =
                new DalvikControlFlowGraph.ExceptionEdge(8, null, 1, 2, handlerLabel);
        Map<Integer, List<DalvikControlFlowGraph.ExceptionEdge>> exceptionalInput = new LinkedHashMap<>();
        List<DalvikControlFlowGraph.ExceptionEdge> exceptionalEdges = new ArrayList<>(List.of(exceptionEdge));
        exceptionalInput.put(4, exceptionalEdges);
        Set<Integer> reachableInput = new LinkedHashSet<>(List.of(4, 7, 8));
        List<DalvikAnalysisFailure> failureInput = new ArrayList<>(List.of(
                new DalvikAnalysisFailure(DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE,
                        4, "snapshot", null)));

        DalvikControlFlowGraph graph = new DalvikControlFlowGraph(4, normalInput, exceptionalInput,
                reachableInput, failureInput);
        normalEdges.clear();
        normalInput.clear();
        exceptionalEdges.clear();
        exceptionalInput.clear();
        reachableInput.clear();
        failureInput.clear();

        assertEquals(List.of(7), graph.normalSuccessors().get(4));
        assertEquals(List.of(exceptionEdge), graph.exceptionalSuccessors().get(4));
        assertEquals(Set.of(4, 7, 8), graph.reachableIndices());
        assertEquals(1, graph.failures().size());
        assertThrows(UnsupportedOperationException.class, () -> graph.normalSuccessors().get(4).clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.exceptionalSuccessors().clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.reachableIndices().clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.failures().clear());
    }

    @Test
    void seedsStaticAndInstanceParametersInPhysicalRegisterWords() {
        Compilation compilation = compile("""
                .super java/lang/Object
                .class public ParameterSeedFixture {
                    .method public static seed (IJDLjava/lang/String;)V {
                        registers: 8,
                        code: {
                            return-void
                        }
                    }
                    .method public instance (J)V {
                        registers: 4,
                        code: {
                            return-void
                        }
                    }
                }
                """);

        DalvikAnalysisResults staticResults = compilation.results("seed", "(IJDLjava/lang/String;)V");
        DalvikRegisterState staticState = entryState(staticResults);
        assertEquals(8, staticState.registerCount());
        assertSame(DalvikRegisterState.Undefined.INSTANCE, staticState.slot(0));
        assertSame(DalvikRegisterState.Undefined.INSTANCE, staticState.slot(1));
        assertEquals(Values.valueOf(PrimitiveType.INT), head(staticState, 2).value());
        assertEquals(Values.valueOf(PrimitiveType.LONG), head(staticState, 3).value());
        assertEquals(new DalvikRegisterState.WideTail(3), staticState.slot(4));
        assertEquals(Values.valueOf(PrimitiveType.DOUBLE), head(staticState, 5).value());
        assertEquals(new DalvikRegisterState.WideTail(5), staticState.slot(6));
        assertEquals(Values.valueOf(new ClassDescriptor("java/lang/String")), head(staticState, 7).value());

        DalvikAnalysisResults instanceResults = compilation.results("instance", "(J)V");
        DalvikRegisterState instanceState = entryState(instanceResults);
        assertEquals(4, instanceState.registerCount());
        assertSame(DalvikRegisterState.Undefined.INSTANCE, instanceState.slot(0));
        assertEquals(Values.valueOfInstance(new ClassDescriptor("ParameterSeedFixture")), head(instanceState, 1).value());
        assertEquals(Values.valueOf(PrimitiveType.LONG), head(instanceState, 2).value());
        assertEquals(new DalvikRegisterState.WideTail(2), instanceState.slot(3));
    }

    @Test
    void seedsConstructorReceiverAsUninitializedAndSupportsZeroRegisters() {
        MethodMember constructor = new MethodMember("<init>", Types.methodTypeFromDescriptor("()V"), 0);
        ReturnInstruction constructorReturn = new ReturnInstruction();
        CodeBuilder constructorBuilder = new CodeBuilder();
        constructorBuilder.add(constructorReturn);
        Code constructorCode = constructorBuilder.arguments(1, 0).registers(1).build();
        DalvikAnalysisResults constructorResults = DalvikAnalysisEngine.analyze(
                new MethodReference("CtorOwner", "<init>", "()V"), constructor, constructorCode, Map.of(), EmptyInheritanceChecker.INSTANCE,
                new BasicMethodValueLookup(), new BasicFieldValueLookup());
        DalvikRegisterState constructorState = entryState(constructorResults);
        assertEquals(new Value.UninitializedReferenceValue(new ClassDescriptor("CtorOwner"), -1),
                head(constructorState, 0).value());

        Compilation compilation = compile("""
                .super java/lang/Object
                .class public ZeroRegister {
                    .method public static zero ()V {
                        registers: 0,
                        code: {
                            return-void
                        }
                    }
                }
                """);
        DalvikRegisterState zeroState = entryState(compilation.results("zero", "()V"));
        assertTrue(zeroState.slots().isEmpty());
    }

    @Test
    void retainsDeadCodeButAnalyzesOnlyReachableInstructions() {
        Compilation compilation = compile("""
                .super java/lang/Object
                .class public DeadCode {
                    .method public static dead ()V {
                        registers: 1,
                        code: {
                            goto Live
                            nop
                        Live:
                            return-void
                        }
                    }
                }
                """);
        DalvikAnalysisResults results = compilation.results("dead", "()V");
        Code code = compilation.method("dead", "()V").getCode();
        DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(code, results.getInstructionToSource());
        int deadIndex = instructionIndex(code, NopInstruction.class::isInstance);

        assertTrue(graph.normalSuccessors().containsKey(deadIndex));
        assertFalse(graph.reachableIndices().contains(deadIndex));
        assertEquals(graph.reachableIndices(), results.getInstructionStates().keySet());
        assertFalse(results.getInstructionStates().containsKey(deadIndex));
    }

    @Test
    void buildsAllNormalSuccessorShapesAtFullCodeIndices() {
        Compilation compilation = compile("""
                .super java/lang/Object
                .class public NormalEdges {
                    .method public static edges (I)V {
                        registers: 1,
                        parameters: { value },
                        code: {
                            const value 0
                            if-eqz value BranchTarget
                            goto GotoTarget
                        BranchTarget:
                            packed-switch value { first: 0, targets: { PackedOne, PackedTwo } }
                            sparse-switch value { 4: SparseFour, -1: SparseNegative }
                            nop
                        GotoTarget:
                            return-void
                        PackedOne:
                            return-void
                        PackedTwo:
                            throw value
                        SparseFour:
                            return-void
                        SparseNegative:
                            return-void
                        }
                    }
                }
                """);
        DalvikAnalysisResults results = compilation.results("edges", "(I)V");
        Code code = compilation.method("edges", "(I)V").getCode();
        DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(code, results.getInstructionToSource());

        ConstInstruction constant = find(code, ConstInstruction.class);
        BranchZeroInstruction branch = find(code, BranchZeroInstruction.class);
        GotoInstruction jump = find(code, GotoInstruction.class);
        PackedSwitchInstruction packed = find(code, PackedSwitchInstruction.class);
        SparseSwitchInstruction sparse = find(code, SparseSwitchInstruction.class);
        NopInstruction nop = find(code, NopInstruction.class);
        ThrowInstruction throwInstruction = find(code, ThrowInstruction.class);

        assertEquals(List.of(nextExecutableIndex(code, indexOf(code, constant))),
                graph.normalSuccessors().get(indexOf(code, constant)));
        assertEquals(List.of(targetIndex(code, branch.label()), nextExecutableIndex(code, indexOf(code, branch))),
                graph.normalSuccessors().get(indexOf(code, branch)));
        assertEquals(List.of(targetIndex(code, jump.jump())), graph.normalSuccessors().get(indexOf(code, jump)));
        assertEquals(List.of(targetIndex(code, packed.targets().get(0)), targetIndex(code, packed.targets().get(1)),
                        nextExecutableIndex(code, indexOf(code, packed))),
                graph.normalSuccessors().get(indexOf(code, packed)));
        List<Label> sparseTargets = new ArrayList<>(new TreeMap<>(sparse.targets()).values());
        assertEquals(List.of(targetIndex(code, sparseTargets.get(0)), targetIndex(code, sparseTargets.get(1)),
                        nextExecutableIndex(code, indexOf(code, sparse))),
                graph.normalSuccessors().get(indexOf(code, sparse)));
        assertEquals(List.of(targetIndex(code, jump.jump())),
                graph.normalSuccessors().get(indexOf(code, nop)));

        for (int index = 0; index < code.getInstructions().size(); index++) {
            Instruction instruction = code.getInstructions().get(index);
            if (instruction instanceof ReturnInstruction || instruction instanceof ThrowInstruction)
                assertTrue(graph.normalSuccessors().get(index).isEmpty(), "terminal at code index " + index);
        }
        assertTrue(graph.normalSuccessors().get(indexOf(code, throwInstruction)).isEmpty());
    }

    @Test
    void emitsOrderedExceptionalEdgesOnlyForPotentiallyThrowingInstructions() {
        Compilation compilation = compile("""
                .super java/lang/Object
                .class public ExceptionalEdges {
                    .method public static guarded ([I)V {
                        registers: 2,
                        parameters: { values },
                        exceptions: {
                            { Start, End, RuntimeHandler, java/lang/RuntimeException },
                            { Start, End, ExceptionHandler, java/lang/Exception },
                            { Start, End, CatchAllHandler, * }
                        },
                        code: {
                        Start:
                            array-length v1 values
                            nop
                        End:
                            return-void
                        RuntimeHandler:
                            move-exception v1
                            return-void
                        ExceptionHandler:
                            move-exception v1
                            return-void
                        CatchAllHandler:
                            move-exception v1
                            return-void
                        }
                    }
                }
                """);
        DalvikAnalysisResults results = compilation.results("guarded", "([I)V");
        Code code = compilation.method("guarded", "([I)V").getCode();
        DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(code, results.getInstructionToSource());
        TryCatch tryCatch = code.tryCatch().getFirst();
        ArrayLengthInstruction arrayLength = find(code, ArrayLengthInstruction.class);
        NopInstruction nop = find(code, NopInstruction.class);
        List<DalvikControlFlowGraph.ExceptionEdge> edges = graph.exceptionalSuccessors().get(indexOf(code, arrayLength));

        assertEquals(3, edges.size());
        assertEquals(List.of(0, 0, 0), edges.stream().map(DalvikControlFlowGraph.ExceptionEdge::tryCatchOrder).toList());
        assertEquals(List.of(0, 1, 2), edges.stream().map(DalvikControlFlowGraph.ExceptionEdge::handlerOrder).toList());
        assertEquals(new ClassDescriptor("java/lang/RuntimeException"), edges.get(0).exceptionType());
        assertEquals(new ClassDescriptor("java/lang/Exception"), edges.get(1).exceptionType());
        assertNull(edges.get(2).exceptionType());
        for (int handler = 0; handler < edges.size(); handler++) {
            assertSame(tryCatch.handlers().get(handler).handler(), edges.get(handler).handlerLabel());
            assertEquals(targetIndex(code, tryCatch.handlers().get(handler).handler()), edges.get(handler).targetIndex());
        }
        assertTrue(graph.exceptionalSuccessors().get(indexOf(code, nop)).isEmpty());
    }

    @Test
    void recordsInvalidGraphAndRegisterLayoutWithoutPublishingEntryState() {
        MethodMember method = new MethodMember("broken", Types.methodTypeFromDescriptor("()V"),
                DalvikModifiers.ACC_STATIC);
        Label absentTarget = new Label();
        GotoInstruction jump = new GotoInstruction(absentTarget);
        CodeBuilder codeBuilder = new CodeBuilder();
        codeBuilder.add(jump);
        codeBuilder.add(new ReturnInstruction());
        Code invalidBranchCode = codeBuilder.arguments(0, 0).registers(0).build();
        DalvikAnalysisResults branchResults = DalvikAnalysisEngine.analyze(
                new MethodReference("InvalidGraph", "broken", "()V"), method, invalidBranchCode, Map.of(), EmptyInheritanceChecker.INSTANCE,
                new BasicMethodValueLookup(), new BasicFieldValueLookup());
        assertTrue(branchResults.getInstructionStates().isEmpty());
        assertEquals("Unresolvable control-flow target label", branchResults.getFailures().getFirst().message());
        assertEquals(DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE,
                branchResults.getFailures().getFirst().kind());

        MethodMember parameterized = new MethodMember("layout", Types.methodTypeFromDescriptor("(I)V"),
                DalvikModifiers.ACC_STATIC);
        CodeBuilder layoutBuilder = new CodeBuilder();
        layoutBuilder.add(new ReturnInstruction());
        Code mismatchedLayoutCode = layoutBuilder.arguments(0, 0).registers(1).build();
        DalvikAnalysisResults layoutResults = DalvikAnalysisEngine.analyze(
                new MethodReference("InvalidLayout", "layout", "(I)V"), parameterized,
                mismatchedLayoutCode, Map.of(), EmptyInheritanceChecker.INSTANCE,
                new BasicMethodValueLookup(), new BasicFieldValueLookup());
        assertTrue(layoutResults.getInstructionStates().isEmpty());
        assertEquals("Method register layout does not match descriptor",
                layoutResults.getFailures().getFirst().message());
        assertEquals(DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE,
                layoutResults.getFailures().getFirst().kind());

        Label begin = new Label();
        Label end = new Label();
        Label absentHandler = new Label();
        CodeBuilder exceptionBuilder = new CodeBuilder();
        exceptionBuilder.add(begin);
        exceptionBuilder.add(new ReturnInstruction());
        exceptionBuilder.add(end);
        Code invalidHandlerCode = exceptionBuilder.arguments(0, 0).registers(0).build();
        invalidHandlerCode.addTryCatch(new TryCatch(begin, end, List.of(new Handler(absentHandler, null))));
        DalvikAnalysisResults handlerResults = DalvikAnalysisEngine.analyze(
                new MethodReference("InvalidHandler", "broken", "()V"), method, invalidHandlerCode, Map.of(), EmptyInheritanceChecker.INSTANCE,
                new BasicMethodValueLookup(), new BasicFieldValueLookup());
        assertTrue(handlerResults.getInstructionStates().isEmpty());
        assertEquals("Unresolvable exception range or handler label",
                handlerResults.getFailures().getFirst().message());
        assertEquals(DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE,
                handlerResults.getFailures().getFirst().kind());

        Label finish = new Label();
        Label start = new Label();
        Label validHandler = new Label();
        CodeBuilder rangeBuilder = new CodeBuilder();
        rangeBuilder.add(finish);
        rangeBuilder.add(new ReturnInstruction());
        rangeBuilder.add(start);
        rangeBuilder.add(new ReturnInstruction());
        rangeBuilder.add(validHandler);
        rangeBuilder.add(new ReturnInstruction());
        Code malformedRangeCode = rangeBuilder.arguments(0, 0).registers(0).build();
        malformedRangeCode.addTryCatch(new TryCatch(start, finish, List.of(new Handler(validHandler, null))));
        DalvikAnalysisResults rangeResults = DalvikAnalysisEngine.analyze(
                new MethodReference("InvalidRange", "broken", "()V"), method, malformedRangeCode, Map.of(), EmptyInheritanceChecker.INSTANCE,
                new BasicMethodValueLookup(), new BasicFieldValueLookup());
        assertTrue(rangeResults.getInstructionStates().isEmpty());
        assertEquals("Unresolvable exception range or handler label",
                rangeResults.getFailures().getFirst().message());
        assertEquals(DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE,
                rangeResults.getFailures().getFirst().kind());
    }

    private static DalvikRegisterState entryState(DalvikAnalysisResults results) {
        assertNotNull(results);
        assertEquals(1, results.getInstructionStates().size(), results.getFailures().toString());
        return results.getInstructionStates().values().iterator().next();
    }

    private static DalvikRegisterState.ValueHead head(DalvikRegisterState state, int register) {
        return assertInstanceOf(DalvikRegisterState.ValueHead.class, state.slot(register));
    }

    private static Compilation compile(String source) {
        List<ASTElement> parsed = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processDeclarations("<dalvik-control-flow-test>", source,
                        DalvikTargetContext.INSTANCE),
                "Dalvik control-flow source should parse");
        ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(parsed, DalvikTargetContext.INSTANCE),
                "Dalvik control-flow source should process");
        Outcome<DalvikCompileResult> outcome = new DalvikCompiler().compile(unit, new DalvikCompilerOptions());
        assertFalse(outcome.hasErrors(), "Unexpected compiler errors: " + outcome.errors());
        assertFalse(outcome.hasWarnings(), "Unexpected compiler warnings: " + outcome.warnings());
        DalvikCompileResult result = outcome.requireValue();
        assertNotNull(result.representation());
        return new Compilation(((DalvikClassRepresentation) result.representation()).definition(), result);
    }

    private static int instructionIndex(Code code, Predicate<Instruction> predicate) {
        for (int index = 0; index < code.getInstructions().size(); index++)
            if (predicate.test(code.getInstructions().get(index)))
                return index;
        throw new AssertionError("Instruction not found in emitted code");
    }

    private static int indexOf(Code code, Instruction instruction) {
        for (int index = 0; index < code.getInstructions().size(); index++)
            if (code.getInstructions().get(index) == instruction)
                return index;
        throw new AssertionError("Instruction is absent from emitted code");
    }

    private static int targetIndex(Code code, Label label) {
        for (int index = 0; index < code.getInstructions().size(); index++) {
            if (code.getInstructions().get(index) != label)
                continue;
            int executable = nextExecutableIndex(code, index - 1);
            if (executable < 0)
                throw new AssertionError("Label does not resolve to executable code");
            return executable;
        }
        throw new AssertionError("Label is absent from emitted code");
    }

    private static int nextExecutableIndex(Code code, int instructionIndex) {
        for (int index = instructionIndex + 1; index < code.getInstructions().size(); index++)
            if (!(code.getInstructions().get(index) instanceof Label))
                return index;
        return -1;
    }

    private static <T extends Instruction> T find(Code code, Class<T> type) {
        return type.cast(code.getInstructions().stream().filter(type::isInstance).findFirst()
                .orElseThrow(() -> new AssertionError("Instruction not found: " + type.getSimpleName())));
    }

    private record Compilation(ClassDefinition definition, DalvikCompileResult result) {
        private DalvikAnalysisResults results(String name, String descriptor) {
            DalvikAnalysisResults results = result.analysisLookup().getResults(name, descriptor);
            assertNotNull(results, name + descriptor);
            return results;
        }

        private MethodMember method(String name, String descriptor) {
            MethodMember method = definition.getMethod(name, descriptor);
            assertNotNull(method, name + descriptor);
            return method;
        }
    }
}
