package me.darknet.assembler.test;

import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.parser.BytecodeFormat;
import org.junit.jupiter.api.Assertions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
	public static JvmCompilation compileJvm(String source, CompilerOptions<?> options) {
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
	public static JvmCompilation compileJvm(String sourceName, String source, CompilerOptions<?> options) {
		var astResult = AssemblyParseFixture.processDeclarations(sourceName, source, BytecodeFormat.JVM);
		if (astResult.hasErr())
			return JvmCompilation.from(sourceName, source, astResult, null);

		List<ASTElement> ast = DiagnosticAssertions.requireOk(astResult, "Failed to prepare AST for JVM compilation");
		JvmCompiler compiler = new JvmCompiler();
		var compileResult = compiler.compile(ast, options);
		if (compileResult.isOk()) {
			verifyGeneratedClass(compiler, compileResult.get().representation());
		}
		return JvmCompilation.from(sourceName, source, astResult, compileResult);
	}

	/**
	 * Verifies that the given class representation can be read and verified as a valid JVM class file.
	 *
	 * @param compiler
	 * 		The compiler that produced the class representation to verify.
	 * @param representation
	 * 		The class representation to verify.
	 */
	private static void verifyGeneratedClass(JvmCompiler compiler, JavaClassRepresentation representation) {
		Assertions.assertNotNull(representation, "Expected generated class bytes");
		byte[] bytes = representation.classFile();
		try {
			compiler.library().read(new ByteArrayInputStream(bytes), new GenericClassBuilder());
		} catch (IOException ex) {
			Assertions.fail("Generated class was not readable", ex);
		}

		StringWriter verifierOutput = new StringWriter();
		try {
			CheckClassAdapter.verify(new ClassReader(bytes), true, new PrintWriter(verifierOutput));
		} catch (Throwable ex) {
			Assertions.fail("Generated class was not verifiable", ex);
		}
	}
}
