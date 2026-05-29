package me.darknet.assembler;

import me.darknet.assembler.compile.visitor.JavaCompileResult;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.JvmAnalysisAssertions;
import me.darknet.assembler.test.JvmCompilation;
import me.darknet.assembler.test.JvmAssemblerFixture;
import me.darknet.assembler.test.SourceNormalization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.function.Consumer;

public class TestUtils {
	/**
	 * Asserts that valid output was emitted with no errors <i>(Warnings are ok though)</i>.
	 *
	 * @param source Jasm source to process.
	 * @param options Jasm compiler options.
	 * @param outputConsumer Consumer to act on the compilation result.
	 */
    public static void processJvm(@NotNull String source, @NotNull CompilerOptions<?> options,
                                  @Nullable ThrowingConsumer<JavaCompileResult> outputConsumer) {
        processJvm(source, options, outputConsumer, null);
    }

	/**
	 * Asserts that valid output was emitted with no errors <i>(Warnings are ok though)</i>.
	 *
	 * @param source Jasm source to process.
	 * @param options Jasm compiler options.
	 * @param outputConsumer Consumer to act on the compilation result.
	 * @param warningConsumer Consumer to act on warnings.
	 */
	public static void processJvm(@NotNull String source, @NotNull CompilerOptions<?> options,
	                              @Nullable ThrowingConsumer<JavaCompileResult> outputConsumer,
	                              @Nullable Consumer<List<Warn>> warningConsumer) {
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
		if (compilation.hasErrors()) {
			fail("Failed to analyze/compile class, errors were reported\n" +
					DiagnosticAssertions.formatErrors(compilation.errors()));
		}
		if (warningConsumer != null && compilation.hasWarnings()) {
			warningConsumer.accept(compilation.warnings());
		}
		try {
			if (outputConsumer != null) {
				outputConsumer.accept(compilation.requireSuccess());
			}
		} catch (AssertionFailedError e) {
			throw e;
		} catch (Throwable e) {
			fail(e);
		}
	}

	/**
	 * Asserts that errors were emitted.
	 *
	 * @param source Jasm source to process.
	 * @param options Jasm compiler options.
	 */
    public static void processAnalysisFailJvm(@NotNull String source, @NotNull CompilerOptions<?> options) {
        JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
        if (!compilation.hasErrors()) {
            fail("Failure was expected");
        }
        JvmAnalysisAssertions.assertAnalysisFailure(compilation);
    }

	/**
	 * Asserts that warnings were emitted.
	 *
	 * @param source Jasm source to process.
	 * @param options Jasm compiler options.
	 */
	public static void processAnalysisWarnJvm(@NotNull String source, @NotNull CompilerOptions<?> options) {
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
		JvmAnalysisAssertions.assertCompileWarning(compilation);
	}

    public static String normalize(String input) {
        return SourceNormalization.normalize(input);
    }
}
