package me.darknet.assembler;

import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static me.darknet.assembler.TestUtils.normalize;
import static me.darknet.assembler.TestUtils.processJvm;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@Disabled(
    "Used to gain a lot of test cases/samples. "
            + "Not practical for test authority due to small syntax changes from the round-trip."
)
public class RuntimeCompilerTest {
    @ParameterizedTest
    @MethodSource("getClasses")
    public void roundTrip(TestArgument argument) throws Throwable {
        // print a class, compile it and print it again. Then compare the two
        JvmClassPrinter printer = new JvmClassPrinter(argument.streamSupplier().get());
        PrintContext<?> ctx = new PrintContext<>("    ");
        printer.print(ctx);
        String printed = ctx.toString();

        JvmCompilerOptions options = new JvmCompilerOptions();
        processJvm(printed, options, classRepresentation -> {
            JvmClassPrinter newPrinter = new JvmClassPrinter(classRepresentation.classFile());
            PrintContext<?> newCtx = new PrintContext<>("    ");
            newPrinter.print(newCtx);
            String newPrinted = newCtx.toString();

            assertEquals(
                    normalize(printed), normalize(newPrinted),
                    "There was an unexpected difference in unmodified class: " + argument.name
            );
        });
    }

    public static List<TestArgument> getClasses() {
        try {
            BiPredicate<Path, BasicFileAttributes> filter = (path, attrib) -> attrib.isRegularFile()
                    && path.toString().endsWith(".class");
            List<TestArgument> collect = Files.find(Paths.get(System.getProperty("user.dir")), 25, filter)
                    .map(p -> new TestArgument(p.getFileName().toString(), () -> Files.newInputStream(p)))
                    .collect(Collectors.toList());
            return collect;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record TestArgument(String name, ThrowingSupplier<InputStream> streamSupplier) {
    }
}
