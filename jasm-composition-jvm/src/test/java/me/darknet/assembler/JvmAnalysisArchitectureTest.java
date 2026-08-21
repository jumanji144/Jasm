package me.darknet.assembler;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.test.BinarySampleFixture;
import me.darknet.assembler.test.JvmAssemblerFixture;
import me.darknet.assembler.test.JvmCompilation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JvmAnalysisArchitectureTest {
    @Test
    void terminalFramesCaptureReturnAndAthrowPaths() {
        String source = BinarySampleFixture.jvmSample("Example-athrow-before-return.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        AnalysisResults results = compilation.requireSuccess().analysisLookup().allResults().values().iterator().next();

        assertNull(results.getAnalysisFailure());
        assertEquals(2, results.terminalFrames().size(), "Expected both areturn and athrow terminal frames");
    }

    @Test
    void frameMergeFailuresExposeFailureKind() {
        String source = BinarySampleFixture.jvmSample("Example-object-int-stack-merge.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        AnalysisResults results = compilation.compileResult().get().analysisLookup().allResults().values().iterator().next();

        assertNotNull(results.getAnalysisFailure());
        assertEquals(AnalysisException.FailureKind.FRAME_MERGE, results.getAnalysisFailure().getKind());
    }

    @Test
    void invalidBranchTargetsExposeFailureKindAndLocation() {
        String source = """
                .super java/lang/Object
                .class Example {
                    .method public example ()V {
                        parameters: { this },
                        code: {
                        A:
                            goto Missing
                            return
                        }
                    }
                }
                """;
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm("missing-label.jasm", source, options);
        AnalysisResults results = compilation.compileResult().get().analysisLookup().allResults().values().iterator().next();

        assertTrue(compilation.hasErrors());
        assertNotNull(results.getAnalysisFailure());
        assertEquals(AnalysisException.FailureKind.INVALID_CONTROL_FLOW, results.getAnalysisFailure().getKind());
        assertEquals(7, compilation.errors().getFirst().getLocation().line());
    }

    @Test
    void engineBugsAreClassifiedSeparately() {
        String source = BinarySampleFixture.jvmSample("Example.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(lookup -> new ThrowingTypedEngine(lookup));

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        AnalysisResults results = compilation.compileResult().get().analysisLookup().allResults().values().iterator().next();

        assertTrue(compilation.hasErrors());
        assertNotNull(results.getAnalysisFailure());
        assertEquals(AnalysisException.FailureKind.ENGINE_BUG, results.getAnalysisFailure().getKind());
    }

    @Test
    void resultMappingsRemainAvailableToConsumers() {
        String source = BinarySampleFixture.jvmSample("Example-comment.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        AnalysisResults results = compilation.requireSuccess().analysisLookup().results("exampleMethod", "()I");

        assertNotNull(results);
        assertFalse(results.getAstToInstructionMap().isEmpty());
        assertFalse(results.getInstructionToAstMap().isEmpty());
        results.getAstToInstructionMap().forEach((ast, element) -> assertSame(ast, results.getInstructionToAstMap().get(element)));
    }

    @Test
    void labelAndLineNumberMappingsRemainAvailableToConsumers() {
        String source = BinarySampleFixture.jvmSample("Example-anewarray-array.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        AnalysisResults results = compilation.requireSuccess().analysisLookup().results("exampleMethod", "()V");
        assertNotNull(results);

        // Every source label must resolve to a LabelNode and back.
        List<String> labelNames = results.getAstToLabelMap().keySet().stream()
                .map(label -> label.identifier().content())
                .sorted()
                .toList();
        assertEquals(List.of("A", "B", "C", "D"), labelNames);
        results.getAstToLabelMap().forEach((ast, node) -> assertSame(ast, results.getLabelToAstMap().get(node)));

        // Every "line <number>" marker must resolve to a LineNumberNode and back.
        List<Integer> lineNumbers = results.getAstToLineNumberMap().values().stream()
                .map(node -> node.line)
                .sorted()
                .toList();
        assertEquals(List.of(3, 4, 5), lineNumbers);
        results.getAstToLineNumberMap().forEach((ast, node) -> assertSame(ast, results.getLineNumberToAstMap().get(node)));

        // 8 executable instructions + 4 labels + 3 line markers must all be present.
        assertEquals(15, results.getAstToInstructionMap().size());
        results.getAstToInstructionMap().forEach((ast, node) -> assertSame(ast, results.getInstructionToAstMap().get(node)));
    }

    @Test
    void framesResolveDirectlyFromInstructionsAndAst() {
        String source = BinarySampleFixture.jvmSample("Example-anewarray-array.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        AnalysisResults results = compilation.requireSuccess().analysisLookup().results("exampleMethod", "()V");
        assertNotNull(results);

        // Each executable instruction resolves to the frame before it, by node and by AST.
        for (Map.Entry<ASTInstruction, AbstractInsnNode> entry : results.getAstToExecutableInstructionMap().entrySet()) {
            ASTInstruction ast = entry.getKey();
            AbstractInsnNode node = entry.getValue();
            Integer index = results.getInstructionIndex(node);
            assertNotNull(index, "Expected an index for " + ast.content());
            Frame frame = results.getFrame(node);
            assertNotNull(frame, "Expected a frame for " + ast.content());
            assertSame(frame, results.frames().floorEntry(index).getValue());
            assertSame(frame, results.getFrame(ast));
        }

        // Labels and line markers have no frame of their own.
        for (ASTLabel label : results.getAstToLabelMap().keySet())
            assertNull(results.getFrame(label));
        for (ASTInstruction line : results.getAstToLineNumberMap().keySet())
            assertNull(results.getFrame(line));
    }

    @Test
    void mergedBranchRevisitStillProducesObjectParameterType() {
        String source = BinarySampleFixture.jvmSample("Example-array-object-merge-on-parameter.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        AnalysisResults results = compilation.requireSuccess().analysisLookup().allResults().values().iterator().next();

        assertNull(results.getAnalysisFailure());
        Frame endFrame = results.frames().lastEntry().getValue();
        assertEquals(me.darknet.assembler.util.JvmTypeUtils.OBJECT, endFrame.getLocalType(0));
    }

    @Test
    void revisitedNewAllocationKeepsStableUninitializedIdentity() {
        // A branch merge that changes an int's value forces a downstream block to be re-analyzed. Re-running the
        // 'new' instruction there must not mint a fresh uninitialized marker, otherwise the re-visit merges two
        // distinct markers for the same allocation into TOP and the constructor/return checks report bogus warnings.
        String source = """
                .super java/lang/Object
                .class public Example {
                    .method public static build (I)LExample; {
                        parameters: { x },
                        code: {
                        A:
                            iload x
                            ifeq B
                            bipush 42
                            goto C
                        B:
                            iload x
                        C:
                            istore y
                        D:
                            new Example
                            dup
                            invokespecial Example.<init> ()V
                            areturn
                        E:
                        }
                    }
                }
                """;
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine::new);

        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);

        assertFalse(compilation.hasErrors(), "Unexpected compilation errors: " + compilation.errors());
        assertTrue(compilation.warnings().isEmpty(), "Expected no warnings, but got: " + compilation.warnings());
    }

    private static final class ThrowingTypedEngine extends TypedJvmAnalysisEngine {
        private ThrowingTypedEngine(VarCache varCache) {
            super(varCache);
        }

        @Override
        public void execute(@NotNull AbstractInsnNode instruction) {
            throw new IllegalStateException("synthetic engine failure");
        }
    }
}
