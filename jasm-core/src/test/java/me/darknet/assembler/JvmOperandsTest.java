package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.instructions.jvm.JvmInstructions;
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

class JvmOperandsTest {

	@Test
	void integerOperandRequiresAnInteger() {
		// Test that integer literals are accepted in decimal, hex, and binary
		assertInstructionOk("""
				.method public static test ()V {
				  code: {
				    sipush 127
				    return
				  }
				}
				""", "sipush");
		assertInstructionOk("""
				.method public static test ()V {
				  code: {
				    sipush 0x7F
				    return
				  }
				}
				""", "sipush");
		assertInstructionOk("""
				.method public static test ()V {
				  code: {
				    sipush 0b01111111
				    return
				  }
				}
				""", "sipush");

		// Test that a non-integer literal fails
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    bipush 1.5
				    return
				  }
				}
				""", "bipush", "integer literal");
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
				""", "invokedynamic", "args");
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
				""", "lookupswitch", "identifier");
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
				""", "tableswitch", "integer literal");
	}

	private static void assertInstructionOk(String source, String instructionName) {
		ProcessorContext context = verify(source, instructionName);
		assertFalse(context.hasErrors(),
				"Expected valid JVM operands but got:\n" + DiagnosticAssertions.formatErrors(context.diagnostics()));
	}

	private static void assertErrorContains(String source, String instructionName, String messagePart) {
		ProcessorContext context = verify(source, instructionName);
		DiagnosticAssertions.assertHasErrorCode(context.diagnostics(), DiagnosticCode.MALFORMED_DECLARATION,
				"Expected JVM operand validation errors");
		DiagnosticAssertions.assertPhase(context.diagnostics(), DiagnosticPhase.TARGET_VALIDATION,
				"Expected JVM operand validation phase");
		assertTrue(DiagnosticAssertions.formatErrors(context.diagnostics()).contains(messagePart),
				DiagnosticAssertions.formatErrors(context.diagnostics()));
	}

	private static ProcessorContext verify(String source, String instructionName) {
		List<ASTElement> elements = DiagnosticAssertions.requireSuccess(
				AssemblyParseFixture.processAst("<test>", source, FixtureTarget.JVM.context()),
				"Failed to process JVM operand source");
		ASTMethod method = assertInstanceOf(ASTMethod.class, elements.getFirst());
		assertNotNull(method.getCode());
		ASTInstruction instruction = method.getCode().getInstructions().stream()
				.filter(candidate -> instructionName.equals(candidate.identifier().content()))
				.findFirst()
				.orElseThrow();
		ProcessorContext context = new ProcessorContext(
				FixtureTarget.JVM.context(), DeclarationRegistry.createDefault(),
				DiagnosticPhase.TARGET_VALIDATION, DiagnosticCode.MALFORMED_DECLARATION);
		var definition = JvmInstructions.INSTANCE.get(instructionName);
		assertNotNull(definition, "No JVM instruction registered for " + instructionName);
		definition.verify(instruction, context);
		return context;
	}


}
