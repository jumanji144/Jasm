package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.jvm.compile.JavaClassRepresentation;
import me.darknet.assembler.backend.jvm.compile.JavaCompileResult;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Outcome;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of compiling a piece of assembly source code into a JVM class representation,
 * including any errors or warnings that occurred during AST processing or compilation.
 *
 * @param sourceName
 * 		The name of the source (File name) for error reporting.
 * @param source
 * 		The JASM assembly source code that was compiled.
 * @param astResult
 * 		The result of processing the JASM assembly source into AST elements, which may contain errors or warnings.
 * @param compileResult
 * 		The result of compiling the AST elements into a class representation, which may contain errors or warnings.
 * @param errors
 * 		A list of errors that occurred during AST processing or compilation.
 * @param warnings
 * 		A list of warnings that occurred during AST processing or compilation.
 *
 * @see JvmAnalysisAssertions
 */
public record JvmCompilation(
		String sourceName,
		String source,
		Outcome<List<ASTElement>> astResult,
		Outcome<JavaCompileResult> compileResult,
		List<Diagnostic> errors,
		List<Diagnostic> warnings
) {
	/**
	 * @return {@code true} if there were any errors during AST processing or compilation, {@code false} otherwise.
	 */
	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	/**
	 * @return {@code true} if there were any warnings during AST processing or compilation, {@code false} otherwise.
	 */
	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

	/**
	 * @return {@code true} if there were no errors during AST processing or compilation,
	 * and a compile result was produced that indicates success, {@code false} otherwise.
	 */
	public boolean isSuccess() {
		return !hasErrors() && compileResult != null && compileResult.isSuccess();
	}

	/**
	 * @return The compile result if compilation succeeded, otherwise throws an assertion error.
	 */
	public JavaCompileResult requireSuccess() {
		Assertions.assertFalse(hasErrors(), "Compilation failed\n" + DiagnosticAssertions.formatErrors(errors));
		Assertions.assertNotNull(compileResult, "No compile result was produced");
		Assertions.assertTrue(compileResult.isSuccess(), "Compilation did not succeed\n" + DiagnosticAssertions.formatErrors(errors));
		JavaCompileResult result = compileResult.requireValue();
		Assertions.assertNotNull(result, "Compilation produced no result");
		Assertions.assertNotNull(result.representation(), "Compilation produced no class representation");
		return result;
	}

	/**
	 * @return The class representation produced by compilation if compilation succeeded, otherwise throws an assertion error.
	 */
	public JavaClassRepresentation requireRepresentation() {
		return requireSuccess().representation();
	}

	/**
	 * @return The class file bytes produced by compilation if compilation succeeded, otherwise throws an assertion error.
	 */
	public byte[] requireClassBytes() {
		return requireRepresentation().classFile();
	}

	/**
	 * @return The disassembly of the compiled class file if compilation succeeded, otherwise throws an assertion error.
	 */
	public String requireDisassembly() {
		return JvmDisassemblyFixture.disassembleJvm(requireClassBytes());
	}

	/**
	 * @return The decompilation of the compiled class file if compilation succeeded, otherwise throws an assertion error.
	 */
	public String requireDecompilation() {
		return JvmDecompilationFixture.decompile(requireClassBytes());
	}

	/**
	 * @param sourceName
	 * 		The name of the source (File name) for error reporting.
	 * @param source
	 * 		The JASM assembly source code that was compiled.
	 * @param astResult
	 * 		The result of processing the JASM assembly source into AST elements, which may contain errors or warnings.
	 * @param compileResult
	 * 		The result of compiling the AST elements into a class representation, which may contain errors or warnings.
	 *
	 * @return Resulting compilation of the given assembly source, including any errors or warnings from both AST processing and compilation.
	 */
	public static JvmCompilation from(String sourceName, String source, Outcome<List<ASTElement>> astResult,
	                                  Outcome<JavaCompileResult> compileResult) {
		List<Diagnostic> errors = new ArrayList<>();
		List<Diagnostic> warnings = new ArrayList<>();
		if (astResult != null) {
			errors.addAll(astResult.errors());
			warnings.addAll(astResult.warnings());
		}
		if (compileResult != null) {
			errors.addAll(compileResult.errors());
			warnings.addAll(compileResult.warnings());
		}
		return new JvmCompilation(sourceName, source, astResult, compileResult, List.copyOf(errors), List.copyOf(warnings));
	}
}
