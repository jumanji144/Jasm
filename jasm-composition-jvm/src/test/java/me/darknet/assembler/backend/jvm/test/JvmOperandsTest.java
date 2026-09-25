package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.instructions.JvmInstructions;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
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

	@Test
	void localOperandsAreNamesOrSlotIndices() {
		// A fractional spelling is neither a declared name nor an index. It used to be coerced into the
		// name "1.5" and silently resolved to a different slot than the source appears to name.
		assertErrorContains("""
				.method public static test (I)V {
				  parameters: { value },
				  code: {
				    iload 1.5
				    return
				  }
				}
				""", "local variable name or index");

		assertErrorContains("""
				.method public static test (I)V {
				  parameters: { value },
				  code: {
				    iinc 1.5 1
				    return
				  }
				}
				""", "local variable name or index");

		// A declared name and an integer index are both legal, in decimal, hex and binary.
		for (String operand : List.of("value", "0", "0x1", "0b1")) {
			assertJvmOk("""
					.method public static test (I)V {
					  parameters: { value },
					  code: {
					    iload %s
					    return
					  }
					}
					""".formatted(operand));
		}

		assertJvmOk("""
				.method public static test (I)V {
				  parameters: { value },
				  code: {
				    iinc value 1
				    return
				  }
				}
				""");
	}

	@Test
	void wideConstantRejectsNarrowValues() {
		// The schema owns the domain the translator needs, so a narrow value can no longer be accepted
		// here and silently emitted as a plain `ldc` by the emitter.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    ldc2_w "value"
				    return
				  }
				}
				""", "Expected wide constant");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    ldc2_w 1
				    return
				  }
				}
				""", "Expected wide constant");

		// A float-suffixed spelling names a 32-bit value, which the wide encoding cannot carry.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    ldc2_w 1.5f
				    return
				  }
				}
				""", "Expected wide constant");
	}

	@Test
	void wideConstantAcceptsLongAndDoubleForms() {
		assertJvmOk("""
				.method public static test ()V {
				  code: {
				    ldc2_w 1L
				    return
				  }
				}
				""");
		assertJvmOk("""
				.method public static test ()V {
				  code: {
				    ldc2_w 1.5
				    return
				  }
				}
				""");
		assertJvmOk("""
				.method public static test ()V {
				  code: {
				    ldc2_w nan
				    return
				  }
				}
				""");
	}

	@Test
	void switchPayloadsDeclareTheirKeySet() {
		// A fixed key set is part of the shape, so an undeclared key is reported rather than ignored.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    tableswitch { min: 3, max: 4, default: D, cases: { A, B }, extra: 1 }
				  A:
				    return
				  B:
				    return
				  D:
				    return
				  }
				}
				""", "keys in table switch");

		// Both switches read their case keys through the one shared parser, so a key that is not an
		// integer is rejected here instead of during lowering.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    lookupswitch { default: D, hello: A }
				  A:
				    return
				  D:
				    return
				  }
				}
				""", "Expected integer switch key");

		// A signed key and a radix-prefixed key are both legal and mean the same thing at every stage.
		assertJvmOk("""
				.method public static test ()V {
				  code: {
				    lookupswitch { default: D, -1: A, 0x10: A, 0b10: A }
				  A:
				    return
				  D:
				    return
				  }
				}
				""");
	}

	@Test
	void tableSwitchMaxMustMatchTheCaseCount() {
		// The emitter derives the upper bound from the case labels, so a wrong `max` used to compile and
		// silently vanish; the payload schema now rejects the disagreement.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    tableswitch { min: 3, max: 99, default: D, cases: { A, B } }
				  A:
				    return
				  B:
				    return
				  D:
				    return
				  }
				}
				""", "Table switch max must be min + cases - 1 (4)");

		assertJvmOk("""
				.method public static test ()V {
				  code: {
				    tableswitch { min: 3, max: 4, default: D, cases: { A, B } }
				  A:
				    return
				  B:
				    return
				  D:
				    return
				  }
				}
				""");
	}

	@Test
	void invokestaticReportsMissingOperandsAndMalformedMemberPaths() {
		record Case(String instruction, DiagnosticCode code, String message) {}

		for (Case test : List.of(
				new Case("invokestatic", DiagnosticCode.OPERAND_SHAPE, "Expected 2 operands, got 0"),
				new Case("invokestatic owner", DiagnosticCode.OPERAND_SHAPE, "Expected 2 operands, got 1"),
				new Case("invokestatic owner.name", DiagnosticCode.OPERAND_SHAPE, "Expected 2 operands, got 1"),
				new Case("invokestatic java/lang/String", DiagnosticCode.OPERAND_SHAPE, "Expected 2 operands, got 1"),
				// Without whitespace, the method reference and descriptor are one source operand.
				new Case("invokestatic java/lang/String.toString()", DiagnosticCode.OPERAND_SHAPE,
						"Expected 2 operands, got 1"),
				// This has two operands, but the member path uses '/' instead of the required '.'.
				new Case("invokestatic java/lang/String/toString() ()Ljava/lang/String;",
						DiagnosticCode.OPERAND_SHAPE, "Expected member path in owner.name form")
		)) {
			Outcome<?> result = processSemantically(invokestaticSource(test.instruction()));
			DiagnosticAssertions.assertHasError(result, test.code(),
					"Expected a diagnostic for: " + test.instruction());
			String errors = DiagnosticAssertions.formatErrors(result.errors());
			assertTrue(errors.contains(test.message()), errors);
		}

		// The documented two-operand form is valid; it must not be classified as missing an operand.
		assertJvmOk(invokestaticSource("invokestatic owner.name ()V"));
	}

	@Test
	void reportsInvalidDescriptorForms() {
		// `new` allocates an instance, so an array type used to reach the emitter and throw a bare
		// IllegalStateException with no source location at all.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    new [I
				    return
				  }
				}
				""", "expected a class type, not an array");

		// A method signature is not a value type, which was previously accepted and emitted as a bogus class name.
		assertErrorContains("""
				.method public static test (Ljava/lang/Object;)V {
				  code: {
				    aload 0
				    checkcast (I)V
				    return
				  }
				}
				""", "method descriptor is not a value type");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    getstatic Example.field Lbad
				    return
				  }
				}
				""", "Expected valid field descriptor but got identifier 'Lbad'");
	}

	private static String invokestaticSource(String instruction) {
		return """
				.method public static test ()V {
				  code: {
				    %s
				    return
				  }
				}
				""".formatted(instruction);
	}

	private static Outcome<?> processSemantically(String source) {
		Outcome<List<ASTElement>> ast = AssemblyParseFixture.processAst("<test>", source, JvmTargetContext.INSTANCE);
		if (ast.hasErrors())
			return ast;
		// Operand definitions are semantic, so run lowering after AST parsing succeeds.
		return SemanticProcessor.process(ast.requireValue(), JvmTargetContext.INSTANCE);
	}

	private static void assertJvmOk(String source) {
		Outcome<?> result = processSemantically(source);
		DiagnosticAssertions.requireSuccess(result, "Expected valid JVM operand input");
	}

	private static void assertErrorContains(String source, String messagePart) {
		Outcome<?> result = processSemantically(source);
		DiagnosticAssertions.assertHasErrors(result, "Expected JVM operand validation errors");
		assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains(messagePart),
				DiagnosticAssertions.formatErrors(result.errors()));
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
