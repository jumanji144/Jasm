package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.instructions.ArrayData;
import me.darknet.assembler.backend.dalvik.instructions.PackedSwitchPayload;
import me.darknet.assembler.backend.dalvik.instructions.RegisterOperands;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRange;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRef;
import me.darknet.assembler.backend.dalvik.instructions.SparseSwitchPayload;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.processing.ProcessedInstruction;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Dalvik operand schemas resolve typed semantic values during processing.
 * <p>
 * These assertions read values through {@code DalvikInstructions} plus {@link SemanticProcessor}
 * only, so a regression that pushes register, member-path or switch-key parsing back into emission
 * fails here rather than surfacing as a raw exception in {@code DalvikCodeVisitor}.
 */
class DalvikSemanticOperandTest {

	@Test
	void resolvesNumericRegisters() {
		ProcessedInstruction instruction = firstInstruction("""
				.method public static test ()V {
				  code: {
				    move v0 v1
				    return-void
				  }
				}
				""");

		assertEquals(new RegisterRef("v0", 0, OperandRole.WidthPolicy.SINGLE), instruction.operand(0, RegisterRef.class));
		assertEquals(new RegisterRef("v1", 1, OperandRole.WidthPolicy.SINGLE), instruction.operand(1, RegisterRef.class));
	}

	@Test
	void resolvesRegisterRangeAndMemberPath() {
		ProcessedInstruction instruction = firstInstruction("""
				.method public static test ()V {
				  code: {
				    invoke-static/range { v2, v3 } java/lang/Math.abs (J)J
				    return-void
				  }
				}
				""");

		RegisterOperands registers = instruction.operand(0, RegisterOperands.class);
		assertTrue(registers.isRange());
		RegisterRange range = (RegisterRange) registers;
		assertEquals(new RegisterRef("v2", 2, OperandRole.WidthPolicy.SINGLE), range.first());
		assertEquals(new RegisterRef("v3", 3, OperandRole.WidthPolicy.SINGLE), range.last());

		MemberPath path = instruction.operand(1, MemberPath.class);
		assertEquals("java/lang/Math", path.owner());
		assertEquals("abs", path.name());
	}

	@Test
	void keepsNamedRegistersUnallocated() {
		ProcessedInstruction instruction = firstInstruction("""
				.method public static test ()V {
				  parameters: { value },
				  code: {
				    check-cast value Ljava/lang/String;
				    return-void
				  }
				}
				""");

		// The slot for a named register is a method-level concern, so processing must leave it null.
		RegisterRef register = instruction.operand(0, RegisterRef.class);
		assertEquals("value", register.name());
		assertNull(register.index());
	}

	@Test
	void wideRegistersDeclareTheirWidth() {
		// A wide value occupies two slots, so the schema that accepted it says so on the resolved value
		// rather than leaving the allocator to infer it from the mnemonic.
		ProcessedInstruction wide = firstInstruction("""
				.method public static test ()V {
				  code: {
				    move-wide v0 v1
				    return-void
				  }
				}
				""");
		assertEquals(OperandRole.WidthPolicy.WIDE, wide.operand(0, RegisterRef.class).width());
		assertEquals(OperandRole.WidthPolicy.WIDE, wide.operand(1, RegisterRef.class).width());

		ProcessedInstruction narrow = firstInstruction("""
				.method public static test ()V {
				  code: {
				    move v0 v1
				    return-void
				  }
				}
				""");
		assertEquals(OperandRole.WidthPolicy.SINGLE, narrow.operand(0, RegisterRef.class).width());
		assertEquals(OperandRole.WidthPolicy.SINGLE, narrow.operand(1, RegisterRef.class).width());
	}

	@Test
	void resolvesArrayDataWithDeclaredWidth() {
		ProcessedInstruction instruction = firstInstruction("""
				.method public static test ()V {
				  code: {
				    fill-array-data v0 { width: 4, values: { 1, 2, 3 } }
				    return-void
				  }
				}
				""");

		ArrayData data = instruction.operand(1, ArrayData.class);
		assertEquals(4, data.elementWidth());
		assertEquals(List.of("1", "2", "3"), data.values().stream().map(ASTElement::content).toList());
	}

	@Test
	void resolvesSwitchPayloadsInAnyRadix() {
		List<ASTElement> ast = DiagnosticAssertions.requireSuccess(
				AssemblyParseFixture.processDeclarations("<test>", """
						.method public static test ()V {
						  code: {
						    packed-switch v0 { first: 0x10, targets: { A } }
						    sparse-switch v1 { -1: A, 0b10: B }
						  A:
						    return-void
						  B:
						    return-void
						  }
						}
						""", DalvikTargetContext.INSTANCE),
				"Source should parse");
		ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
				SemanticProcessor.process(ast, DalvikTargetContext.INSTANCE), "Source should process semantically");
		List<me.darknet.assembler.processing.ProcessedCodeEntry> code =
				unit.methods().values().iterator().next().code();

		ProcessedInstruction packed = (ProcessedInstruction) code.get(0);
		PackedSwitchPayload packedPayload = packed.operand(1, PackedSwitchPayload.class);
		assertEquals(16, packedPayload.first());
		assertEquals(List.of("A"), packedPayload.targets());

		ProcessedInstruction sparse = (ProcessedInstruction) code.get(1);
		SparseSwitchPayload sparsePayload = sparse.operand(1, SparseSwitchPayload.class);
		// -1 proves sign handling and 0b10 proves radix handling moved to processing.
		assertEquals("A", sparsePayload.targets().get(-1));
		assertEquals("B", sparsePayload.targets().get(2));
	}

	@Test
	void reportsMalformedOperandsAsDiagnostics() {
		// Every one of these previously escaped processing as a raw exception wrapped at emission time.
		assertDiagnostic("""
				.method public static test ()V {
				  registers: 4,
				  code: {
				    move v0 v99999999999
				    return-void
				  }
				}
				""", "Invalid register name: v99999999999");

		assertDiagnostic("""
				.method public static test ()V {
				  registers: 4,
				  code: {
				    invoke-static/range { } java/lang/Math.abs (J)J
				    return-void
				  }
				}
				""", "Range instructions require first and last register bounds");

		assertDiagnostic("""
				.method public static test ()V {
				  registers: 4,
				  code: {
				    invoke-static/range { v1, v0 } java/lang/Math.abs (J)J
				    return-void
				  }
				}
				""", "Range instruction register bounds are reversed");

		assertDiagnostic("""
				.method public static test ()V {
				  registers: 4,
				  code: {
				    invoke-static { v0 } java/lang/Math (J)J
				    return-void
				  }
				}
				""", "Expected member path in owner.name form: java/lang");

		assertDiagnostic("""
				.method public static test ()V {
				  registers: 4,
				  code: {
				    sparse-switch v0 { zz: L }
				  L:
				    return-void
				  }
				}
				""", "Expected integer switch key");
	}

	/**
	 * @param source
	 * 		JASM source expected to fail during semantic processing.
	 * @param messagePart
	 * 		Fragment the reported diagnostics must contain.
	 */
	private static void assertDiagnostic(String source, String messagePart) {
		var result = AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE);
		if (result.hasErrors()) {
			assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains(messagePart),
					DiagnosticAssertions.formatErrors(result.errors()));
			return;
		}
		var processed = SemanticProcessor.process(result.requireValue(), DalvikTargetContext.INSTANCE);
		DiagnosticAssertions.assertHasErrors(processed, "Source should fail semantic processing");
		assertTrue(DiagnosticAssertions.formatErrors(processed.errors()).contains(messagePart),
				DiagnosticAssertions.formatErrors(processed.errors()));
	}

	/**
	 * @param source
	 * 		JASM source containing one method whose first code entry is a single instruction.
	 *
	 * @return Processed view of that first instruction.
	 */
	private static ProcessedInstruction firstInstruction(String source) {
		List<ASTElement> ast = DiagnosticAssertions.requireSuccess(
				AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE),
				"Source should parse");
		ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
				SemanticProcessor.process(ast, DalvikTargetContext.INSTANCE), "Source should process semantically");
		ProcessedInstruction instruction = (ProcessedInstruction) unit.methods().values().iterator().next()
				.code().getFirst();
		assertNotNull(instruction, "Method body should carry a processed instruction");
		return instruction;
	}
}
