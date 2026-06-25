package me.darknet.assembler;

import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import static me.darknet.assembler.test.AstAssertions.assertProcessedDalvikOk;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikOperandsTest {

	@Test
	void packedSwitchAcceptsDifferentIntegerFormats() {
		assertProcessedDalvikOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 127, targets: { L0 } }
				    return-void
				  }
				}
				""");
		assertProcessedDalvikOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 0x7F, targets: { L0 } }
				    return-void
				  }
				}
				""");
		assertProcessedDalvikOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 0b01111111, targets: { L0 } }
				    return-void
				  }
				}
				""");
	}

	@Test
	void packedSwitchRequiresAnIntegerFirstKey() {
		// A double is not valid as a key
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 1.5, targets: { L0 } }
				    return-void
				  }
				}
				""", "integer literal");
	}

	@Test
	void invokeCustomRequiresAnArgsArray() {
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    invoke-custom { v0 } callsite ()V { invokestatic, example/Bootstrap.bootstrap, ()V } bad
				    return-void
				  }
				}
				""", "args array");
	}

	@Test
	void filledNewArrayRequiresRegisterIdentifiers() {
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array { 1 } [I
				    return-void
				  }
				}
				""", "register");
	}

	private static void assertErrorContains(String source, String messagePart) {
		Result<?> result = AssemblyParseFixture.processAst("<test>", source, BytecodeFormat.DALVIK);
		DiagnosticAssertions.assertHasErrors(result, "Expected Dalvik operand validation errors");
		assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains(messagePart));
	}
}
