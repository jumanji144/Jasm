package me.darknet.assembler.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.parser.BytecodeFormat;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utilities for asserting properties of AST processing results in tests.
 */
public final class AstAssertions {
	private AstAssertions() {}

	/**
	 * @param input
	 * 		The JASM assembly source code to process.
	 * @param clazz
	 * 		The expected class of the single AST node produced by processing.
	 * @param consumer
	 * 		A consumer that will be called with the single AST node if processing succeeds and produces exactly one node.
	 * 		Can be used to make further assertions on the node.
	 * @param <T>
	 * 		The type of the expected AST node.
	 */
	public static <T extends ASTElement> void assertOneProcessed(String input, Class<T> clazz, Consumer<T> consumer) {
		List<ASTElement> results = DiagnosticAssertions.requireOk(
				AssemblyParseFixture.processAst(input),
				"Failed to process AST"
		);
		Assertions.assertEquals(1, results.size(), "Expected exactly one AST node");
		consumer.accept(assertIs(clazz, results.getFirst()));
	}

	/**
	 * Verifies that processing the given JASM assembly source code succeeds without errors.
	 *
	 * @param input
	 * 		The JASM assembly source code to process.
	 */
	public static void assertProcessedJvmOk(String input) {
		DiagnosticAssertions.requireOk(
				AssemblyParseFixture.processAst(AssemblyParseFixture.STDIN, input, BytecodeFormat.JVM),
				"Failed to process AST"
		);
	}

	/**
	 * Verifies that processing the given JASM assembly source code succeeds without errors.
	 *
	 * @param input
	 * 		The JASM assembly source code to process.
	 */
	public static void assertProcessedDalvikOk(String input) {
		DiagnosticAssertions.requireOk(
				AssemblyParseFixture.processAst(AssemblyParseFixture.STDIN, input, BytecodeFormat.DALVIK),
				"Failed to process AST"
		);
	}

	/**
	 * @param input
	 * 		The JASM assembly source code to process.
	 * @param errorConsumer
	 * 		A consumer that will be called with the list of errors if processing fails.
	 * 		Can be used to make assertions on the errors.
	 */
	public static void assertProcessedError(String input, Consumer<List<Error>> errorConsumer) {
		var result = AssemblyParseFixture.processAst(input);
		Assertions.assertTrue(result.hasErr(), "Expected errors but processing succeeded");
		errorConsumer.accept(result.errors());
	}

	/**
	 * @param shouldBe
	 * 		The expected class of the object.
	 * @param is
	 * 		The object to check.
	 * @param <T>
	 * 		The type of the expected class.
	 *
	 * @return The object cast to the expected class if it is an instance of that class, otherwise throws an assertion error.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T assertIs(Class<T> shouldBe, Object is) {
		Assertions.assertNotNull(is);
		Assertions.assertInstanceOf(shouldBe, is);
		return (T) is;
	}
}
