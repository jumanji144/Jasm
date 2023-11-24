package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.jvm.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static me.darknet.assembler.TestUtils.normalize;
import static me.darknet.assembler.TestUtils.processJvm;
import static org.junit.jupiter.api.Assertions.*;

public class SampleCompilerTest {
	private static final String PATH_PREFIX = "src/test/resources/samples/jasm/";

	@Nested
	class Variables {
		@Test
		void basic() throws Throwable {
			TestArgument arg = TestArgument.fromName("Example-variables.jasm");
			String source = arg.source.get();
			processJvm(source, new TestJvmCompilerOptions(), classRepresentation -> {
				AnalysisResults results = classRepresentation.analysisLookup().allResults().values().iterator().next();
				assertNull(results.getAnalysisFailure());
				Set<String> varNames = results.frames().values().stream()
						.flatMap(Frame::locals)
						.map(Local::name)
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
		@ValueSource(strings = {
				"Example-int-multi.jasm",
				"Example-int-addition.jasm",
				"Example-int-division.jasm",
				"Example-int-multiplication.jasm",
				"Example-int-remainder.jasm",
				"Example-int-subtraction.jasm",
		})
		void intMath(String name) throws Throwable {
			TestArgument arg = TestArgument.fromName(name);
			String source = arg.source.get();
			TestJvmCompilerOptions options = new TestJvmCompilerOptions();
			options.engineProvider(ValuedJvmAnalysisEngine::new);
			processJvm(source, options, classRepresentation -> {
				AnalysisResults results = classRepresentation.analysisLookup().allResults().values().iterator().next();
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
			processJvm(source, options, classRepresentation -> {
				AnalysisResults results = classRepresentation.analysisLookup().allResults().values().iterator().next();
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
			processJvm(source, options, classRepresentation -> {
				AnalysisResults results = classRepresentation.analysisLookup().allResults().values().iterator().next();
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
	}

	@ParameterizedTest
	@MethodSource("getSources")
	void roundTrip(TestArgument arg) throws Throwable {
		String source = arg.source.get();
		processJvm(source, new TestJvmCompilerOptions(), classRepresentation -> {
			if (source.contains("SKIP-ROUND-TRIP-EQUALITY")) return;

			JvmClassPrinter newPrinter = new JvmClassPrinter(classRepresentation.classFile());
			PrintContext<?> newCtx = new PrintContext<>("    ");
			newPrinter.print(newCtx);
			String newPrinted = newCtx.toString();

			assertEquals(normalize(source), normalize(newPrinted), "There was an unexpected difference in unmodified class: " + arg.name);
		});
	}

	public static List<TestArgument> getSources() {
		try {
			BiPredicate<Path, BasicFileAttributes> filter =
					(path, attrib) -> attrib.isRegularFile() && path.toString().endsWith(".jasm");
			return Files.find(Paths.get(System.getProperty("user.dir")).resolve("src"), 25, filter)
					.map(TestArgument::from)
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private record TestArgument(String name, ThrowingSupplier<String> source) {
		public static TestArgument fromName(String name) {
			return from(Paths.get(System.getProperty("user.dir"))
					.resolve(PATH_PREFIX)
					.resolve(name));
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
