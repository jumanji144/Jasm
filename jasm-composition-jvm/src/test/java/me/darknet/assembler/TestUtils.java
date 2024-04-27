package me.darknet.assembler;

import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compile.visitor.JavaCompileResult;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.parser.BytecodeFormat;

import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class TestUtils {
    private static final Pattern DUPLICATE_NEWLINES = Pattern.compile("\\n\\s*\\n");
    private static final Pattern END_LINE_PADDING = Pattern.compile("[ \\t]+\\n");
    private static final Pattern COMMENTS = Pattern.compile("(?:^|\\n)\\s*//.+");

    public static void processJvm(@NotNull String source, @NotNull CompilerOptions<?> options,
                                  @Nullable ThrowingConsumer<JavaCompileResult> outputConsumer) {
        Processor.processSource(source, "<test>", (ast) -> {
            JvmCompiler compiler = new JvmCompiler();
            compiler.compile(ast, options).ifOk(result -> {
                try {
                    if (outputConsumer != null)
                        outputConsumer.accept(result);
                } catch (AssertionFailedError e) {
                    // Pass up the chain
                    throw e;
                } catch (Throwable e) {
                    // Consumer should fail instead of us handling it generically here
                    fail(e);
                    return;
                }

	            // Check if bytes are a valid class file
	            JavaClassRepresentation representation = result.representation();
	            byte[] bytes = representation.classFile();
	            try {
		            compiler.library().read(new ByteArrayInputStream(bytes), new GenericClassBuilder());
	            } catch (IOException e) {
		            fail("Generated class was not readable", e);
	            }

	            // And double check that its verifiable
	            try {
		            CheckClassAdapter.verify(
				            new ClassReader(bytes),
				            true,
				            new PrintWriter(System.out)
		            );
	            } catch (Throwable e) {
		            fail("Generated class was not verifiable", e);
	            }


            }).ifErr(errors -> {
                for (Error error : errors) {
                    System.err.println(error);
                }
                fail("Failed to analyze/compile class");
            });
        }, errors -> {
            for (Error error : errors) {
                System.err.println(error);
            }
            fail("Failed to parse class");
        }, BytecodeFormat.JVM);
    }

    public static void processAnalysisFailJvm(@NotNull String source, @NotNull CompilerOptions<?> options,
                                              @Nullable ThrowingConsumer<JavaCompileResult> outputConsumer) {
        Processor.processSource(source, "<test>", (ast) -> {
            JvmCompiler compiler = new JvmCompiler();
            compiler.compile(ast, options).ifOk(result -> {
                fail("Failure was expected");
            }).ifErr((result, errors) -> {
                try {
                    if (outputConsumer != null)
                        outputConsumer.accept(result);
                } catch (AssertionFailedError e) {
                    // Pass up the chain
                    throw e;
                } catch (Throwable e) {
                    // Consumer should fail instead of us handling it generically here
                    fail(e);
                }
            });
        }, errors -> {
            // We expect to parse the class, but for analysis to fail
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
