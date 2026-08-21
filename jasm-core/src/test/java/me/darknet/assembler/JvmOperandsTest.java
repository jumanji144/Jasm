package me.darknet.assembler;

import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import static me.darknet.assembler.test.AstAssertions.assertProcessedJvmOk;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmOperandsTest {

	@Test
	void integerOperandRequiresAnInteger() {
		// Test that integer literals are accepted in decimal, hex, and binary
		assertProcessedJvmOk("""
				.method public static test ()V {
				  code: {
				    sipush 127
				    return
				  }
				}
				""");
		assertProcessedJvmOk("""
				.method public static test ()V {
				  code: {
				    sipush 0x7F
				    return
				  }
				}
				""");
		assertProcessedJvmOk("""
				.method public static test ()V {
				  code: {
				    sipush 0b01111111
				    return
				  }
				}
				""");

		// Test that a non-integer literal fails
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    bipush 1.5
				    return
				  }
				}
				""", "integer literal");
	}

	@Test
	void invokedynamicRequiresAnArgsArray() {
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    invokedynamic run ()V { invokestatic, example/Bootstrap.bootstrap, ()V } bad
				    return
				  }
				}
				""", "args");
	}

	@Test
	void lookupSwitchRequiresLabelTargets() {
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    lookupswitch { default: L0, 0: 1 }
				    return
				  }
				}
				""", "identifier");
	}

	@Test
	void tableSwitchRequiresIntegerBounds() {
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    tableswitch { min: 1.5, max: 2, default: L0, cases: { L1 } }
				    return
				  }
				}
				""", "integer literal");
	}

	private static void assertErrorContains(String source, String messagePart) {
		Result<?> result = AssemblyParseFixture.processAst("<test>", source, BytecodeFormat.JVM);
		DiagnosticAssertions.assertHasErrors(result, "Expected JVM operand validation errors");
		assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains(messagePart));
	}


}
