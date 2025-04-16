package me.darknet.assembler;

import dev.xdark.blw.code.instruction.MethodInstruction;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.jvm.MethodValueLookup;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;

import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import static me.darknet.assembler.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.runtime.SwitchBootstraps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SampleCompilerTest {
    private static final String PATH_PREFIX = "src/test/resources/samples/jasm/";
    private static final String PATH_BIN_PREFIX = "src/test/resources/samples/binary/";
    private static final String PATH_ILLEGAL_PREFIX = "src/test/resources/samples/jasm-illegal/";

    /**
     * For testing variable outputs.
     */
    @Nested
    class Variables {
        @Test
        void basic() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-variables.jasm");
            String source = arg.source.get();
            processJvm(source, new TestJvmCompilerOptions(), result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                Set<String> varNames = results.frames().values().stream().flatMap(Frame::locals).map(Local::name)
                        .collect(Collectors.toSet());
                assertTrue(varNames.contains("this"));
                assertTrue(varNames.contains("other"));
                assertTrue(varNames.contains("hundred"));
                assertTrue(varNames.contains("fifty"));
                assertTrue(varNames.contains("result"));
                assertTrue(varNames.contains("msPerTick"));
                assertTrue(varNames.contains("ex"));
            });
        }
    }

    /**
     * For testing analysis engine capabilities.
     */
    @Nested
    class Analysis {
        @ParameterizedTest
        @ValueSource(
                strings = { "Example-int-multi.jasm", "Example-int-addition.jasm", "Example-int-division.jasm",
                        "Example-int-iinc.jasm", "Example-int-multiplication.jasm", "Example-int-remainder.jasm",
                        "Example-int-subtraction.jasm", }
        )
        void intMath(String name) throws Throwable {
            TestArgument arg = TestArgument.fromName(name);
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
                results.terminalFrames().values().stream().map(f -> (ValuedFrame) f).forEach(frame -> {
                    Value returnValue = frame.peek();
                    if (returnValue instanceof Value.KnownIntValue known)
                        assertEquals(100, known.value());
                    else
                        fail("Unexpected ret-val: " + returnValue);
                });
            });
        }

        @Test
        void ldcPushType() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-push-type.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());

                ValuedFrame frame = (ValuedFrame) results.terminalFrames().values().iterator().next();
                if (frame.peek() instanceof Value.ObjectValue objectValue) {
                    assertEquals(Types.instanceType(Class.class), objectValue.type(), "Pushing type to stack did not yield class reference");
                } else {
                    fail("Did not yield object value");
                }
            });
        }

        @Test
        void methodLookup() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-string-ops.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(lookup -> {
                ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(lookup);
                engine.setMethodValueLookup(new BasicMethodValueLookup());
                return engine;
            });
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
                results.terminalFrames().values().stream().map(f -> (ValuedFrame) f).forEach(frame -> {
                    Value returnValue = frame.peek();
                    if (returnValue instanceof Value.KnownIntValue known)
                        assertEquals(100, known.value());
                    else
                        fail("Unexpected ret-val: " + returnValue);
                });
            });
        }

        @Test
        void fieldLookup() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-getstatic.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(lookup -> {
                ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(lookup);
                engine.setFieldValueLookup((instruction, context) -> Values.valueOf(100));
                return engine;
            });
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
                results.terminalFrames().values().stream().map(f -> (ValuedFrame) f).forEach(frame -> {
                    Value returnValue = frame.peek();
                    if (returnValue instanceof Value.KnownIntValue known)
                        assertEquals(100, known.value());
                    else
                        fail("Unexpected ret-val: " + returnValue);
                });
            });
        }

        @Test
        void typeInferenceListForValuedAnalysis() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-type-infer-list.jasm");
            String source = arg.source.get();
            listTypeInference(source, options -> options.engineProvider(ValuedJvmAnalysisEngine::new));
        }

        @Test
        void typeInferenceListForValuedAnalysisAlt() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-type-infer-list-alt.jasm");
            String source = arg.source.get();
            listTypeInference(source, options -> options.engineProvider(ValuedJvmAnalysisEngine::new));
        }

        @Test
        void typeInferenceListForTypedAnalysis() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-type-infer-list.jasm");
            String source = arg.source.get();
            listTypeInference(source, options -> options.engineProvider(TypedJvmAnalysisEngine::new));
        }

        @Test
        void typeInferenceListForTypedAnalysisAlt() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-type-infer-list-alt.jasm");
            String source = arg.source.get();
            listTypeInference(source, options -> options.engineProvider(TypedJvmAnalysisEngine::new));
        }

        @SuppressWarnings("DataFlowIssue")
        void listTypeInference(@NotNull String source, @Nullable Consumer<TestJvmCompilerOptions> optionsConsumer) {
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.inheritanceChecker(new ReflectiveInheritanceChecker(getClass().getClassLoader()));
            if (optionsConsumer != null) optionsConsumer.accept(options);
            processJvm(source, options, result -> {
                ClassReader reader = new ClassReader(result.representation().classFile());
                ClassNode node = new ClassNode();
                reader.accept(node, 0);


                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                NavigableMap<Integer, Frame> frames = results.frames();
                Frame lastFrame = frames.lastEntry().getValue();
                Local local = lastFrame.locals()
                        .filter(l -> l.name().equals("c"))
                        .findFirst().orElse(null);
                 assertNotNull(local, "The 'c' local was not found");
                 assertEquals(Types.instanceType(List.class), local.type(), "Expected 'c' == List.class");
            });
        }
    }

    /**
     * For cases known to emit errors.
     */
    @Nested
    class Error {
        @Test
        void putfieldWithoutContext() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-putfield-no-context.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            options.inheritanceChecker(new ReflectiveInheritanceChecker(getClass().getClassLoader()));
            processAnalysisFailJvm(source, options);
        }

        @Test
        void intAndObjectStackMerge() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-object-int-stack-merge.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            options.inheritanceChecker(new ReflectiveInheritanceChecker(getClass().getClassLoader()));
            processAnalysisFailJvm(source, options);
        }

        @Test
        void intAndObjectLocalMerge() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-int-object-var-merge.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            options.inheritanceChecker(new ReflectiveInheritanceChecker(getClass().getClassLoader()));
            processAnalysisFailJvm(source, options);
        }

        @Test
        @Disabled("Checking for uninitialized vars is more than a linear search. " +
                "We cannot realistically include this as part of the single-pass" +
                "analysis-simulation")
        void loadUninitializedVariable() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-load-not-initialized.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            options.inheritanceChecker(new ReflectiveInheritanceChecker(getClass().getClassLoader()));
            processAnalysisFailJvm(source, options);
        }
    }

    /**
     * For cases known to emit warnings, but not strictly errors.
     */
    @Nested
    class Warning {
        @Test
        void wrongReturn() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-wrong-return.jasm"));
        }

        @Test
        void int2Object() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-int2obj-cast.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-int2obj-instanceof.jasm"));
        }

        @Test
        void null2Int() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-null2int-ineg.jasm"));
        }

        @Test
        void object2Int() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-obj2int-iadd.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-obj2int-f2i.jasm"));
        }

        @Test
        void switchOnNull() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-tswitch-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-lswitch-null.jasm"));
        }

        @Test
        void switchOnObj() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-tswitch-obj.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-lswitch-obj.jasm"));
        }

        @Test
        void getField() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-getfield-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-getfield-prim.jasm"));
        }

        @Test
        void putField() throws Throwable {
            // Field contexts
            warnOnBothEngines(TestArgument.fromName("Example-putfield-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-putfield-prim.jasm"));

            // Array stack values
            warnOnBothEngines(TestArgument.fromName("Example-putstatic-array-to-prim.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-putstatic-array-to-wrong-component-type.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-putstatic-array-to-wrong-dim-array.jasm"));

            // Primitive stack values
            warnOnBothEngines(TestArgument.fromName("Example-putstatic-prim-to-array.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-putstatic-prim-to-obj.jasm"));

            // Instance stack values
            warnOnBothEngines(TestArgument.fromName("Example-putstatic-obj-to-prim.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-putstatic-obj-to-array.jasm"));
        }

        @Test
        void invokeContext() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-invokevirtual-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-invokevirtual-prim.jasm"));
        }

        @Test
        void storeTypeIncompatibility() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-istore-obj.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-istore-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-astore-int.jasm"));
        }

        @Test
        void arrays() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-arraylen-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arraylen-int.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arraylen-obj.jasm"));

            warnOnBothEngines(TestArgument.fromName("Example-arrayload-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arrayload-int.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arrayload-obj.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arrayload-index.jasm"));

            warnOnBothEngines(TestArgument.fromName("Example-arraystore-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arraystore-int.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arraystore-obj.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-arraystore-index.jasm"));
        }

        @Test
        void math() throws Throwable {
            warnOnBothEngines(TestArgument.fromName("Example-fneg-null.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-fcmpl-null-a.jasm"));
            warnOnBothEngines(TestArgument.fromName("Example-fcmpl-null-b.jasm"));
        }

        void warnOnBothEngines(TestArgument arg) throws Throwable {
            // Both analysis engines should have warnings for these cases
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.inheritanceChecker(new ReflectiveInheritanceChecker(getClass().getClassLoader()));

            // Simpler type-only stack analysis engine
            options.engineProvider(TypedJvmAnalysisEngine::new);
            processAnalysisWarnJvm(source, options);

            // Value stack analysis engine
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processAnalysisWarnJvm(source, options);
        }
    }

    /**
     * Cases that came up as bug reports. Exist to ensure we do not regress in behavior and re-introduce bugs.
     */
    @Nested
    class Regresssion {
        @Test
        void varDifferentiationWithoutDebugSymbols() throws Throwable {
            byte[] buf = Files.readAllBytes(Path.of(PATH_BIN_PREFIX + "same-var-slot-diff-types.sample"));
            JvmClassPrinter classPrinter = new JvmClassPrinter(new ByteArrayInputStream(buf));
            PrintContext<PrintContext<?>> ctx = new PrintContext<>("  ");
            classPrinter.print(ctx);
            String source = ctx.toString();

            // Assert the constructor printed OK
            assertTrue(Pattern.compile("aload this\\n\\s+invokespecial java\\/lang\\/Object\\.<init> \\(\\)V").matcher(source).find(),
                    "Constructor did not properly emit 'aload this' + 'invokespecial super'");

            // Assert the primitive 'consume(T)' calls printed OK
            char[] prims = new char[]{'I', 'F', 'D', 'J'};
            for (char prim : prims) {
                char lowerPrim = Character.toLowerCase(prim);
                char insnPrim = lowerPrim;
                if (insnPrim == 'j') insnPrim = 'l'; // Edge case for longs
                assertTrue(Pattern.compile(insnPrim + "store " + lowerPrim + "0\\n\\s+" + insnPrim +
                                "load " + lowerPrim + "0\\n\\s+invokestatic Vars\\.consume \\(" + prim + "\\)V").matcher(source).find(),
                        "Did not properly emit 'const_T' + 'tstore 0', 'tload 0', 'invoke consume(T)' for T=" + prim);
            }

            // Assert the object 'consume(T)' printed OK
            assertTrue(Pattern.compile("astore v0\\n\\s+aload v0\\n\\s+invokestatic Vars\\.consume \\(Ljava\\/lang\\/Object;\\)V").matcher(source).find(),
                    "Did not properly emit 'aload 0' + 'invoke consume(A)'");

            // The bug before was any variable slot would fall back to 'vN' for N being the var slot.
            // This would lead 'astore N' and 'istore N' in classes without variable symbols to emit
            // in JASM 'astore vN' and 'istore vN' which would cause frame merge errors since two incompatible
            // types were occupying the same space.
            //
            // The fix is to differentiate generated names based on type.
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            });
        }

        @Test
        void spacesAndCommentsDoNotBreakAstReportedLocations() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-comment.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults methodAnalysis = result.analysisLookup().results("exampleMethod", "()I");
                assertNotNull(methodAnalysis);
                Set<ASTInstruction> instructionAstNodes = methodAnalysis.getAstToCodeMap().keySet();
                for (ASTInstruction astNode : instructionAstNodes) {
                    if ("istore".equals(astNode.identifier().content()) ) {
                        // The istore should appear on line 23
                        Location location = astNode.location();
                        assertNotNull(location);
                        assertEquals(23, location.line());
                    } else  if ("iload".equals(astNode.identifier().content()) ) {
                        // The istore should appear on line 36
                        Location location = astNode.location();
                        assertNotNull(location);
                        assertEquals(36, location.line());
                    }
                }
            });
        }

        @Test
        void tryWithResourceVariableScopeConfusion_Valued() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-try-with-resources.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            }, warns -> {
                warns.forEach(System.err::println);
                fail("No warnings allowed");
            });
        }

        @Test
        void tryWithResourceVariableScopeConfusion_Typed() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-try-with-resources.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(TypedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            }, warns -> {
                warns.forEach(System.err::println);
                fail("No warnings allowed");
            });
        }

        @Test
        void newArrayPopsSizeOffStack() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-anewarray.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            });
        }

        @Test
        void arrayLoadAndStores() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-varied-array-ops.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();

            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            });

            // Again with the other engine
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            });
        }

        @Test
        void athrowDoesNotAllowFlowThroughToNextFrameAndClearsStack() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-exit-exception.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            });
        }

        @Test
        void stackPopForInvokes() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-wide-invoke.jasm");
            String source = arg.source.get();

            // Create an analysis engine which will observe the invokestatic method in the source.
            // If wide types are mishandled it will not get visited.
            boolean[] visited = new boolean[1];
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(lookup -> {
                ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(lookup);
                engine.setMethodValueLookup(new MethodValueLookup() {
                    @Override
                    public @NotNull Value accept(@NotNull MethodInstruction instruction, Value.@Nullable ObjectValue context, @NotNull List<Value> parameters) {
                        visited[0] = true;
                        return Values.LONG_VALUE;
                    }
                });
                return engine;
            });

            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());
            }, warns -> {
                // Void type usage in the engine for method parameters should emit a warning.
                // If this occurs we've broken something.
                fail("Expected no warnings, found: " + warns);
            });
            assertTrue(visited[0], "Method call was not visited");
        }

        @Test
        void checkcastChangesType() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-checkcast.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.inheritanceChecker(ReflectiveInheritanceChecker.INSTANCE);
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                assertFalse(results.terminalFrames().isEmpty());

                Frame endFrame = results.terminalFrames().lastEntry().getValue();
                if (endFrame instanceof ValuedFrame valuedEndFrame) {
                    Value returnValue = valuedEndFrame.peek();
                    assertEquals(Types.instanceType(List.class), returnValue.type());
                } else {
                    fail("Wrong return value");
                }
            });
        }

        @Test
        void arrayObjectMergeOnParameter() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-array-object-merge-on-parameter.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                // should not fail or produce warning and p0 should be of type Object
                assertNull(results.getAnalysisFailure());

                ClassType p0Type = results.frames().lastEntry().getValue().getLocalType(0);
                assertEquals(Types.OBJECT, p0Type);
            }, warns -> {
                // Void type usage in the engine for method parameters should emit a warning.
                // If this occurs we've broken something.
                fail("Expected no warnings, found: " + warns);
            });
        }
    }

    /**
     * Assemble, disassemble, compare against original input. They should be identical (barring whitespace).
     */
    @Nested
    class RoundTrip {
        @ParameterizedTest
        @MethodSource("getValidSources")
        void all(TestArgument arg) throws Throwable {
            String source = arg.source.get();
            processJvm(source, new TestJvmCompilerOptions(), result -> {
                if (source.contains("SKIP-ROUND-TRIP-EQUALITY"))
                    return;

                String newPrinted = dissassemble(result.representation().classFile());

                assertEquals(
                        normalize(source), normalize(newPrinted),
                        "There was an unexpected difference in unmodified class: " + arg.name
                );
            });
        }

        @Test
        @Disabled
        void kotlinSr2c() throws Throwable {
            String source = getValidSources().stream().filter(t -> t.name.contains("KKKSample")).findFirst().get().source().get();
            processJvm(source, new TestJvmCompilerOptions(), result -> {
                /*
                String newPrinted = dissassemble(result.representation().classFile());

                assertEquals(
                        normalize(source), normalize(newPrinted),
                        "There was an unexpected difference in unmodified class: " + arg.name
                );*/
            });
        }
        @Test
        @Disabled
        void kotlinSrc() throws Throwable {
            String source = getValidSources().stream().filter(t -> t.name.contains("KotlinSample")).findFirst().get().source().get();
            processJvm(source, new TestJvmCompilerOptions(), result -> {
                /*
                String newPrinted = dissassemble(result.representation().classFile());

                assertEquals(
                        normalize(source), normalize(newPrinted),
                        "There was an unexpected difference in unmodified class: " + arg.name
                );*/
            });
        }

        @Test
       //@Disabled
        void kotlin() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("ExtrasConfig.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Round-trip it
            processJvm(source, new TestJvmCompilerOptions(), result -> {
               // String newPrinted = dissassemble(result.representation().classFile());

              //  assertEquals(normalize(source), normalize(newPrinted), "There was an unexpected difference in unmodified class: " + arg.name);
            });
        }

	    @ParameterizedTest
	    @ValueSource(strings = {
			    "u000d.sample", "u000a.sample", "u0009.sample", "u2028.sample",
			    "u002c.sample", "u002e.sample", "u0000.sample", "u0001.sample",
			    "u0002.sample", "u0003.sample", "u0004.sample", "u0005.sample",
			    "u0006.sample", "u0007.sample", "u0008.sample", "u0009.sample",
			    "u000a.sample", "u000b.sample", "u000c.sample", "u000d.sample",
			    "u000e.sample", "u000f.sample", "u0010.sample", "u0011.sample",
			    "u0012.sample", "u0013.sample", "u0014.sample", "u2000.sample",
			    "u2001.sample", "u2002.sample", "u2003.sample", "u2004.sample",
			    "u2005.sample", "u2006.sample", "u2007.sample", "u2008.sample",
			    "u2009.sample", "u200a.sample",
	    })
	    void unicodeEscapeRoundTrip(String name) throws Throwable {
		    BinaryTestArgument arg = BinaryTestArgument.fromName(name);
		    byte[] raw = arg.source.get();
		    String source = dissassemble(raw);
		    processJvm(source, new TestJvmCompilerOptions(), result -> {
			    String newPrinted = dissassemble(result.representation().classFile());
			    assertEquals(
					    normalize(source), normalize(newPrinted),
					    "There was an unexpected difference in unmodified class: " + arg.name
			    );
		    });
	    }

        /**
         * Infinity should remain as a printed constant across re-assembles
         */
        @Test
        void supportInfinity() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-infinity.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                String newPrinted = dissassemble(result.representation().classFile());

                assertEquals(normalize(source.replace("InfinityD", "Infinity").replace("+", "")), normalize(newPrinted));
            });
        }

        /**
         * NaN should remain as a printed constant across re-assembles
         */
        @Test
        void supportNan() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-nan.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                String newPrinted = dissassemble(result.representation().classFile());

                assertEquals(normalize(source.replace("NaND", "NaN")), normalize(newPrinted));
            });
        }

        public static List<TestArgument> getSources() {
            try {
                BiPredicate<Path, BasicFileAttributes> filter = (path, attrib) -> attrib.isRegularFile()
                        && path.toString().endsWith(".jasm");
                return Files.find(Paths.get(System.getProperty("user.dir")).resolve("src"), 25, filter)
                        .map(TestArgument::from).collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static List<TestArgument> getValidSources() {
            return getSources().stream()
                    .filter(a -> !a.path().toString().contains("illegal"))
                    .toList();
        }
    }

    @Nested
    class AttributeSupport {
        @Test
        void defaultAnnotationValue() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("AnnoDefaultValues.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Assert all the default values are there
            assertTrue(source.contains("default-value: 0"));
            assertTrue(source.contains("default-value: 'c'"));
            assertTrue(source.contains("default-value: \"hello\""));
            assertTrue(source.contains("default-value: { 0, 1, 2 }"));
            assertTrue(source.contains("default-value: .enum java/lang/annotation/ElementType FIELD"));
            assertTrue(source.contains("default-value: .annotation java/lang/annotation/Retention {"));
            assertTrue(source.contains("    value: .enum java/lang/annotation/RetentionPolicy CLASS"));

            // Round-trip it
            roundTrip(source, arg);
        }

        @Test
        void outerClassInfo() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("Outside$1.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Assert it has the expected outside class attribute information (including nest-host)
            assertTrue(source.contains(".nest-host Outside"));
            assertTrue(source.contains(".outer-class Outside"));
            assertTrue(source.contains(".outer-method run ()V "));

            // Round-trip it
            roundTrip(source, arg);
        }

        @Test
        void innerClassInfo() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("Outside.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Assert it has the expected inner class attribute information (including nest-member)
            assertTrue(source.contains("""
                    .inner {
                        inner: Outside$1
                    }"""));
            assertTrue(source.contains(".nest-member Outside$1"));

            // Round-trip it
            roundTrip(source, arg);
        }

        @Test
        void pickCorrectlyTypedParameterName() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("RedHerringVarEntries.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Ensure we extract the correct variable name from the table.
            // The table has bogus in it as defined by:
            //
            // 0: new Variable("vWrongF", "F", start, end, 0);
            // 1: new Variable("vWrongObject", "Ljava/lang/Object;", null, start, end, 0);
            // 2: new Variable("vCorrect", "I", start, end, 0);
            // 3: new Variable("vWrongD", "D", start, end, 0);
            assertTrue(source.contains("parameters: { vCorrect }"));
            assertTrue(source.contains("iload vCorrect"));

            // Round-trip it
            roundTrip(source, arg);
        }

        @Test
        void permittedSubclasses() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("SubclassTest.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Assert it has the permitted subclass attribute
            assertTrue(source.contains(".permitted-subclass foo/ImplA"));
            assertTrue(source.contains(".permitted-subclass foo/ImplB"));

            // Round-trip it
            roundTrip(source, arg);
        }

        @Test
        void recordComponents() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("ExampleRecord.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Assert it has the record component attributes
            assertTrue(source.contains(".record-component foo I"));
            assertTrue(source.contains(".record-component bar J"));
            assertTrue(source.contains(".record-component s Ljava/lang/String;"));

            // Round-trip it
            roundTrip(source, arg);
        }

        @Test
        void recordComponentWithGenerics() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("GenericRecord.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Assert it has the record component attributes and the generic attribute before it.
            assertTrue(source.contains(".signature \"Ljava/util/Map<TK;TV;>;\""));
            assertTrue(source.contains(".record-component map Ljava/util/Map;"));

            // The class should keep its overall signature as well.
            assertTrue(source.contains(".signature \"<K::Ljava/lang/Comparable<*>;V:Ljava/lang/Object;>Ljava/lang/Record;\""));

            // Round-trip it
            roundTrip(source, arg);
        }

        @Test
        void recordComponentWithAnnos() throws Throwable {
            BinaryTestArgument arg = BinaryTestArgument.fromName("AnnotatedRecord.sample");
            byte[] raw = arg.source.get();

            // Print the initial raw
            String source = dissassemble(raw);

            // Assert both annotations on the record component AND the class are kept.
            assertTrue(source.contains("""
                    .visible-annotation Marked {
                        value: "class"
                    }"""));
            assertTrue(source.contains("""
                    .visible-annotation Marked {
                        value: "param"
                    }"""));
            assertTrue(source.contains(".record-component s Ljava/lang/String; "));

            // Round-trip it
            roundTrip(source, arg);
        }

        private static void roundTrip(String source, BinaryTestArgument arg) {
            processJvm(source, new TestJvmCompilerOptions(), result -> {
                String newPrinted = dissassemble(result.representation().classFile());

                assertEquals(
                        normalize(source), normalize(newPrinted),
                        "There was an unexpected difference in unmodified class: " + arg.name
                );
            });
        }
    }

    private static String dissassemble(byte[] raw) throws IOException {
        JvmClassPrinter initPrinter = new JvmClassPrinter(raw);
        PrintContext<?> initCtx = new PrintContext<>("    ");
        initPrinter.print(initCtx);
        return initCtx.toString();
    }


    record TestArgument(Path path, String name, ThrowingSupplier<String> source) {
        public static TestArgument fromName(String name) {
            Path path = Paths.get(System.getProperty("user.dir")).resolve(PATH_PREFIX).resolve(name);
            if (!Files.exists(path))
                path = Paths.get(System.getProperty("user.dir")).resolve(PATH_ILLEGAL_PREFIX).resolve(name);
            return from(path);
        }

        public static TestArgument from(Path path) {
            return new TestArgument(path, path.getFileName().toString(), () -> Files.readString(path));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record BinaryTestArgument(Path path, String name, ThrowingSupplier<byte[]> source) {
        public static BinaryTestArgument fromName(String name) {
            Path path = Paths.get(System.getProperty("user.dir")).resolve(PATH_BIN_PREFIX).resolve(name);
            return from(path);
        }

        public static BinaryTestArgument from(Path path) {
            return new BinaryTestArgument(path, path.getFileName().toString(), () -> Files.readAllBytes(path));
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
