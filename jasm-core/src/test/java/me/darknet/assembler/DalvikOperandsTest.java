package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.instructions.dalvik.DalvikInstructions;
import me.darknet.assembler.parser.processor.DeclarationRegistry;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.FixtureTarget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikOperandsTest {

	@Test
	void packedSwitchAcceptsDifferentIntegerFormats() {
		assertInstructionOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 127, targets: { L0 } }
				    return-void
				  }
				}
				""", "packed-switch");
		assertInstructionOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 0x7F, targets: { L0 } }
				    return-void
				  }
				}
				""", "packed-switch");
		assertInstructionOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 0b01111111, targets: { L0 } }
				    return-void
				  }
				}
				""", "packed-switch");
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
				""", "packed-switch", "integer literal");
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
				""", "invoke-custom", "args array");
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
				""", "filled-new-array", "register");
	}

	private static void assertInstructionOk(String source, String instructionName) {
		ProcessorContext context = verify(source, instructionName);
		assertFalse(context.hasErrors(),
				"Expected valid Dalvik operands but got:\n" + DiagnosticAssertions.formatErrors(context.diagnostics()));
	}

	private static void assertErrorContains(String source, String instructionName, String messagePart) {
		ProcessorContext context = verify(source, instructionName);
		DiagnosticAssertions.assertHasErrorCode(context.diagnostics(), DiagnosticCode.MALFORMED_DECLARATION,
				"Expected Dalvik operand validation errors");
		DiagnosticAssertions.assertPhase(context.diagnostics(), DiagnosticPhase.TARGET_VALIDATION,
				"Expected Dalvik operand validation phase");
		assertTrue(DiagnosticAssertions.formatErrors(context.diagnostics()).contains(messagePart),
				DiagnosticAssertions.formatErrors(context.diagnostics()));
	}

	private static ProcessorContext verify(String source, String instructionName) {
		List<ASTElement> elements = DiagnosticAssertions.requireSuccess(
				AssemblyParseFixture.processAst("<test>", source, FixtureTarget.DALVIK.context()),
				"Failed to process Dalvik operand source");
		ASTMethod method = assertInstanceOf(ASTMethod.class, elements.getFirst());
		assertNotNull(method.getCode());
		ASTInstruction instruction = method.getCode().getInstructions().stream()
				.filter(candidate -> instructionName.equals(candidate.identifier().content()))
				.findFirst()
				.orElseThrow();
		ProcessorContext context = new ProcessorContext(
				FixtureTarget.DALVIK.context(), DeclarationRegistry.createDefault(),
				DiagnosticPhase.TARGET_VALIDATION, DiagnosticCode.MALFORMED_DECLARATION);
		var definition = DalvikInstructions.INSTANCE.get(instructionName);
		assertNotNull(definition, "No Dalvik instruction registered for " + instructionName);
		definition.verify(instruction, context);
		return context;
	}
}
