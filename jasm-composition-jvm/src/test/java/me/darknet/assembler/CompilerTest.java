package me.darknet.assembler;

import dev.xdark.blw.classfile.ClassBuilder;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class CompilerTest {

    @ParameterizedTest
    @MethodSource("me.darknet.assembler.CompilerTest#getClasses")
    public void roundTrip(File source) throws IOException {
        // print a class, compile it and print it again. Then compare the two

        JvmClassPrinter printer = new JvmClassPrinter(Files.newInputStream(source.toPath()));
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
                    JvmCompiler.library.read(new ByteArrayInputStream(bytes), ClassBuilder.builder());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).ifErr((errors) -> {
                for (Error error : errors) {
                    System.err.println(error);
                }
                Assertions.fail("Failed to compile class");
            });
        }, (errors) -> {
            for (Error error : errors) {
                System.err.println(error);
            }
            Assertions.fail("Failed to parse class");
        }, BytecodeFormat.JVM);
    }

    public static List<File> getClasses() {
        return Arrays.asList(
                Paths.get("out/production/classes/me/darknet/assembler/compile/BlwCompiler.class").toFile()
        );
    }

}
