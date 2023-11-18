package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Frame;
import me.darknet.assembler.compile.analysis.LocalInfo;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SampleCompilerTest {
	@Nested
	class Variables {
		@Test
		void basic() throws Throwable {
			TestArgument arg = TestArgument.from(Paths.get(System.getProperty("user.dir"))
					.resolve("src/test/resources/samples/jasm/Example-variables.jasm"));
			String source = arg.source.get();
			processJvm(source, new TestJvmCompilerOptions(), classRepresentation -> {
				AnalysisResults results = classRepresentation.analysisLookup().allResults().values().iterator().next();
				Frame lastFrame = results.frames().lastEntry().getValue();
				Set<String> varNames = lastFrame.getLocals().values().stream()
						.map(LocalInfo::name)
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

	@ParameterizedTest
	@MethodSource("getSources")
	void roundTrip(TestArgument arg) throws Throwable {
		String source = arg.source.get();
		processJvm(source, new TestJvmCompilerOptions(), classRepresentation -> {
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
		public static TestArgument from(Path path) {
			return new TestArgument(path.getFileName().toString(), () -> Files.readString(path));
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
