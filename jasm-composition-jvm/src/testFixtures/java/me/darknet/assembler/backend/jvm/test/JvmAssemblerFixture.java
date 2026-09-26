package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.compile.JavaClassRepresentation;
import me.darknet.assembler.backend.jvm.compile.JavaCompileResult;
import me.darknet.assembler.backend.jvm.compile.JvmCompiler;
import me.darknet.assembler.backend.jvm.compile.JvmCompilerOptions;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import org.junit.jupiter.api.Assertions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Utilities for compiling assembly source code into JVM class representations in tests, including verification of the generated class.
 */
public final class JvmAssemblerFixture {
	private JvmAssemblerFixture() {}

	/**
	 * @param source
	 * 		The JASM assembly source code to compile.
	 * @param options
	 * 		The compiler options to use for compilation.
	 *
	 * @return A {@link JvmCompilation} containing the results of compiling the given source code with the given options.
	 */
	public static JvmCompilation compileJvm(String source, JvmCompilerOptions options) {
		return compileJvm("<test>", source, options);
	}

	/**
	 * @param sourceName
	 * 		The name of the source (File name) for error reporting.
	 * @param source
	 * 		The JASM assembly source code to compile.
	 * @param options
	 * 		The compiler options to use for compilation.
	 *
	 * @return A {@link JvmCompilation} containing the results of compiling the given source code with the given options.
	 */
	public static JvmCompilation compileJvm(String sourceName, String source, JvmCompilerOptions options) {
		Outcome<List<ASTElement>> astResult = AssemblyParseFixture.processDeclarations(sourceName, source, JvmTargetContext.INSTANCE);
		if (astResult.hasErrors())
			return JvmCompilation.from(sourceName, source, astResult, null);

		List<ASTElement> ast = DiagnosticAssertions.requireSuccess(astResult, "Failed to prepare AST for JVM compilation");
		Outcome<ValidatedUnit> processingResult = SemanticProcessor.process(ast, JvmTargetContext.INSTANCE);
		if (processingResult.hasErrors())
			return JvmCompilation.from(sourceName, source, astResult, null, processingResult.diagnostics());

		JvmCompiler compiler = new JvmCompiler();
		Outcome<JavaCompileResult> compileResult = compiler.compile(processingResult.requireValue(), options);
		if (compileResult.isSuccess()) {
			verifyGeneratedClass(compileResult.requireValue().representation());
		}
		return JvmCompilation.from(sourceName, source, astResult, compileResult, processingResult.diagnostics());
	}

	/**
	 * Verifies that the given class representation can be read and verified as a valid JVM class file.
	 *
	 * @param representation
	 * 		The class representation to verify.
	 */
	private static void verifyGeneratedClass(JavaClassRepresentation representation) {
		Assertions.assertNotNull(representation, "Expected generated class bytes");
		byte[] bytes = representation.classFile();
		Assertions.assertDoesNotThrow(() -> new ClassReader(bytes), "Generated class was not readable");

		StringWriter verifierOutput = new StringWriter();
		try {
			CheckClassAdapter.verify(new ClassReader(bytes), true, new PrintWriter(verifierOutput));
		} catch (Throwable ex) {
			Assertions.fail("Generated class was not verifiable", ex);
		}
	}
}
