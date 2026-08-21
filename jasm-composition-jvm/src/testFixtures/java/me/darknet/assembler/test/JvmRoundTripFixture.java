package me.darknet.assembler.test;

import me.darknet.assembler.compiler.CompilerOptions;

/**
 * Utilities for performing a round-trip of compiling JVM assembly source code into a JVM class representation
 * and then disassembling it back into JASM source code in tests.
 */
public final class JvmRoundTripFixture {
	private JvmRoundTripFixture() {}

	/**
	 * Performs a round-trip test by compiling the given JVM assembly source code into a JVM class representation
	 *
	 * @param source
	 * 		The JASM assembly source code to compile and disassemble.
	 * @param options
	 * 		The compiler options to use for compilation, allowing customization of the compilation process for the round-trip test.
	 *
	 * @return A {@link JvmRoundTripResult} containing both the results of the compilation and the disassembled JASM source code, which can be used for assertions in tests.
	 */
	public static JvmRoundTripResult roundTripJvm(String source, CompilerOptions<?> options) {
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
		return new JvmRoundTripResult(compilation, compilation.requireDisassembly());
	}

	/**
	 * Container for the results of a JVM round-trip test, containing both the compilation results and the disassembled source code.
	 *
	 * @param compilation
	 * 		The results of compiling the original JVM assembly source code, including any errors or warnings.
	 * @param disassembledSource
	 * 		The JASM source code obtained by disassembling the generated JVM class file from the compilation step, which can be used for assertions in tests.
	 */
	public record JvmRoundTripResult(JvmCompilation compilation, String disassembledSource) {}
}
