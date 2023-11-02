package me.darknet.assembler;

import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static me.darknet.assembler.TestUtils.normalize;
import static me.darknet.assembler.TestUtils.processJvm;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SampleCompilerTest {
	@ParameterizedTest
	@MethodSource("getSources")
	public void roundTrip(TestArgument arg) throws Throwable {
		JvmCompilerOptions options = new JvmCompilerOptions();
		options.inheritanceChecker(new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				return false;
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				return "java/lang/Object";
			}
		});
		String source = arg.source.get();
		processJvm(source, options, classRepresentation -> {
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
			List<TestArgument> collect = Files.find(Paths.get(System.getProperty("user.dir")).resolve("src"), 25, filter)
					.map(p -> new TestArgument(p.getFileName().toString(), () -> Files.readString(p)))
					.collect(Collectors.toList());
			return collect;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private record TestArgument(String name, ThrowingSupplier<String> source) {
		@Override
		public String toString() {
			return name;
		}
	}
}
