package me.darknet.assembler;

import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.parser.BytecodeFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {
    private static final Pattern DUPLICATE_NEWLINES = Pattern.compile("\\n\\s*\\n");
    private static final Pattern END_LINE_PADDING = Pattern.compile("[ \\t]+\\n");
    private static final Pattern COMMENTS = Pattern.compile("(?:^|\\n)\\s*//.+");

    public static void processJvm(@NotNull String source, @NotNull CompilerOptions<?> options,
            @Nullable ThrowingConsumer<JavaClassRepresentation> outputConsumer) {
        Processor.processSource(source, "<test>", (ast) -> {
            JvmCompiler compiler = new JvmCompiler();
            compiler.compile(ast, options).ifOk(representation -> {
                try {
                    if (outputConsumer != null)
                        outputConsumer.accept(representation);
                } catch (Throwable e) {
                    // Consumer should fail instead of us handling it generically here
                    fail(e);
                    return;
                }

                // Check if bytes are valid
                try {
                    byte[] bytes = representation.classFile();
                    compiler.library().read(new ByteArrayInputStream(bytes), new GenericClassBuilder());
                } catch (IOException e) {
                    fail("Generated class was not readable", e);
                }
            }).ifErr(errors -> {
                for (Error error : errors) {
                    System.err.println(error);
                }
                fail("Failed to compile class");
            });
        }, errors -> {
            for (Error error : errors) {
                System.err.println(error);
            }
            fail("Failed to parse class");
        }, BytecodeFormat.JVM);
    }

    public static String normalize(String input) {
        input = input.replace("\r", "");
        while (input.contains("  "))
            input = input.replace("  ", " ");

        input = COMMENTS.matcher(input).replaceAll("");
        input = END_LINE_PADDING.matcher(input).replaceAll("\n");
        input = DUPLICATE_NEWLINES.matcher(input).replaceAll("\n");
        return input.trim();
    }
}
