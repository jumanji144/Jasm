package me.darknet.assembler;

import dev.xdark.blw.classfile.ClassBuilder;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class CompilerTest {
    @ParameterizedTest
    @MethodSource("me.darknet.assembler.CompilerTest#getClasses")
    public void roundTrip(TestSource source) throws Throwable {
        // print a class, compile it and print it again. Then compare the two

        JvmClassPrinter printer = new JvmClassPrinter(source.streamSupplier().get());
        JvmCompilerOptions options = new JvmCompilerOptions();

        PrintContext<?> ctx = new PrintContext<>("    ");

        printer.print(ctx);

        String first = ctx.toString();

        Processor.processSource(first, "<test>", (ast) -> {
            JvmCompiler compiler = new JvmCompiler();
            compiler.compile(ast, options).ifOk((representation) -> {
                byte[] bytes = representation.data();

                // check if bytes are valid
                try {
                    compiler.library().read(new ByteArrayInputStream(bytes), ClassBuilder.builder());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).ifErr((errors) -> {
                for (Error error : errors) {
                    System.err.println(error);
                }
                fail("Failed to compile class");
            });
        }, (errors) -> {
            for (Error error : errors) {
                System.err.println(error);
            }
            fail("Failed to parse class");
        }, BytecodeFormat.JVM);
    }

    public static List<TestSource> getClasses() {
        try {
            BiPredicate<Path, BasicFileAttributes> filter =
                    (path, attrib) -> attrib.isRegularFile() && path.toString().endsWith(".class");
            List<TestSource> collect = Files.find(Paths.get(System.getProperty("user.dir")), 25, filter)
                    .map(p -> new TestSource(p.getFileName().toString(), () -> Files.newInputStream(p)))
                    .collect(Collectors.toList());
            return collect;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record TestSource(String name, UncheckedSupplier<InputStream> streamSupplier) {
    }

    private interface UncheckedSupplier<T> {
        T get() throws Throwable;
    }
}
