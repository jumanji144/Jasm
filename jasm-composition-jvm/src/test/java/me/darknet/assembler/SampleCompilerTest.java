package me.darknet.assembler;

import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static me.darknet.assembler.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SampleCompilerTest {
    private static final String PATH_PREFIX = "src/test/resources/samples/jasm/";
    private static final String PATH_ILLEGAL_PREFIX = "src/test/resources/samples/jasm-illegal/";

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
        void typeInferenceListForTypedAnalysis() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-type-infer-list.jasm");
            String source = arg.source.get();
            listTypeInference(source, options -> options.engineProvider(TypedJvmAnalysisEngine::new));
        }

        void listTypeInference(@NotNull String source, @Nullable Consumer<TestJvmCompilerOptions> optionsConsumer) {
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.inheritanceChecker(new ReflectiveInheritanceChecker(getClass().getClassLoader()));
            if (optionsConsumer != null) optionsConsumer.accept(options);
            processJvm(source, options, result -> {
                AnalysisResults results = result.analysisLookup().allResults().values().iterator().next();
                assertNull(results.getAnalysisFailure());
                NavigableMap<Integer, Frame> frames = results.frames();
                Frame lastFrame = frames.lastEntry().getValue();
                Local local = lastFrame.locals()
                        .filter(l -> l.name().equals("c"))
                        .findFirst().orElse(null);
                assertNotNull(local, "The 'c' local was not found");
                assertEquals(local.type(), Types.instanceType(List.class), "Expected 'c' == List.class");
            });
        }
    }

    @Nested
    class Illegal {
        @Test
        void intAndObjectStackMerge() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-object-int-stack-merge.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processAnalysisFailJvm(source, options, result -> {

            });
        }
    }

    @Nested
    class Regresssion {
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
    }

    @Nested
    class RoundTrip {
        @ParameterizedTest
        @MethodSource("getSources")
        void all(TestArgument arg) throws Throwable {
            String source = arg.source.get();
            processJvm(source, new TestJvmCompilerOptions(), result -> {
                if (source.contains("SKIP-ROUND-TRIP-EQUALITY"))
                    return;

                JvmClassPrinter newPrinter = new JvmClassPrinter(result.representation().classFile());
                PrintContext<?> newCtx = new PrintContext<>("    ");
                newPrinter.print(newCtx);
                String newPrinted = newCtx.toString();

                assertEquals(
                        normalize(source), normalize(newPrinted),
                        "There was an unexpected difference in unmodified class: " + arg.name
                );
            });
        }

        @Test
        void supportInfinity() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-infinity.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                JvmClassPrinter newPrinter = new JvmClassPrinter(result.representation().classFile());
                PrintContext<?> newCtx = new PrintContext<>("    ");
                newPrinter.print(newCtx);
                String newPrinted = newCtx.toString();

                assertEquals(normalize(source.replace("InfinityD", "Infinity").replace("+", "")), normalize(newPrinted));
            });
        }

        @Test
        void supportNan() throws Throwable {
            TestArgument arg = TestArgument.fromName("Example-nan.jasm");
            String source = arg.source.get();
            TestJvmCompilerOptions options = new TestJvmCompilerOptions();
            options.engineProvider(ValuedJvmAnalysisEngine::new);
            processJvm(source, options, result -> {
                JvmClassPrinter newPrinter = new JvmClassPrinter(result.representation().classFile());
                PrintContext<?> newCtx = new PrintContext<>("    ");
                newPrinter.print(newCtx);
                String newPrinted = newCtx.toString();

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
    }

    private record TestArgument(String name, ThrowingSupplier<String> source) {
        public static TestArgument fromName(String name) {
            Path path = Paths.get(System.getProperty("user.dir")).resolve(PATH_PREFIX).resolve(name);
            if (!Files.exists(path))
                path = Paths.get(System.getProperty("user.dir")).resolve(PATH_ILLEGAL_PREFIX).resolve(name);
            return from(path);
        }

        public static TestArgument from(Path path) {
            return new TestArgument(path.getFileName().toString(), () -> Files.readString(path));
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
