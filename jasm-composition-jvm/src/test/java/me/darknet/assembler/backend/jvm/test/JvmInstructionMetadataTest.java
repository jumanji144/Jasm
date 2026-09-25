package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.TestJvmCompilerOptions;
import me.darknet.assembler.TestUtils;
import me.darknet.assembler.backend.jvm.instructions.JvmInstructions;
import me.darknet.assembler.backend.jvm.instructions.JvmLowering;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.util.JvmOpcodes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies completeness of JVM instruction metadata declarations.
 */
class JvmInstructionMetadataTest {
	@Test
	void knownAliasAndTypeCasesRemainExplicit() {
		assertEquals(0, JvmInstructions.INSTANCE.get("multianewarray").typeReferenceOperandIndex());
		assertEquals("ldc", JvmInstructions.INSTANCE.get("ldc2_w").canonicalName());
	}

	@Test
	void everyInstructionHasRequiredMetadata() {
		for (String name : JvmInstructions.INSTANCE.getInstructionNames()) {
			Instruction<?> instruction = JvmInstructions.INSTANCE.get(name);
			assertNotNull(instruction, name);
			assertTrue(instruction.lowering() instanceof JvmLowering || "line".equals(name), name);
			// A branch names its target, and leaving the instruction and falling through are
			// mutually exclusive outcomes.
			if (instruction.hasTrait(InstructionTrait.CONDITIONAL_BRANCH)
					|| instruction.hasTrait(InstructionTrait.UNCONDITIONAL_BRANCH))
				assertTrue(instruction.metadata().roles().stream()
						.anyMatch(role -> role.kind() == OperandRole.RoleKind.LABEL), name);
			if (instruction.hasTrait(InstructionTrait.FALLTHROUGH))
				assertFalse(instruction.hasTrait(InstructionTrait.UNCONDITIONAL_BRANCH)
						|| instruction.hasTrait(InstructionTrait.RETURN)
						|| instruction.hasTrait(InstructionTrait.THROW)
						|| instruction.hasTrait(InstructionTrait.SWITCH), name);
			if (instruction.hasTrait(InstructionTrait.TYPE_REFERENCE))
				assertTrue(instruction.typeReferenceOperandIndex() >= 0, name);
			for (OperandRole role : instruction.metadata().roles())
				assertEquals(referencePolicyFor(role.kind()), role.referencePolicy(), name);
			if (instruction.hasTrait(InstructionTrait.FIELD_REFERENCE)
					|| instruction.hasTrait(InstructionTrait.METHOD_REFERENCE))
				assertTrue(instruction.memberReferenceOperandIndex() >= 0, name);
			if (instruction.hasTrait(InstructionTrait.SWITCH))
				assertNotNull(instruction.switchShape(), name);
		}
	}

	@Test
	void memberReferenceOperandIsDeclared() {
		// The trait says the instruction references a member; the role says which operand carries it, so
		// a consumer can resolve the reference without reading the translator's parameter types.
		for (String name : List.of("getstatic", "putstatic", "getfield", "putfield"))
			assertEquals(0, JvmInstructions.INSTANCE.get(name).memberReferenceOperandIndex(), name);
		for (String name : List.of("invokevirtual", "invokespecial", "invokestatic", "invokeinterface",
				"invokevirtualinterface", "invokestaticinterface", "invokespecialinterface"))
			assertEquals(0, JvmInstructions.INSTANCE.get(name).memberReferenceOperandIndex(), name);

		// invokedynamic names a bootstrap method through a handle, so it declares no member path.
		assertEquals(-1, JvmInstructions.INSTANCE.get("invokedynamic").memberReferenceOperandIndex());
	}

	@Test
	void variableRolesCoverReadsWritesAndIncrements() {
		// The trait says which direction a local is accessed and the role says which operand carries it,
		// so a consumer can tell a load from a store without matching on the mnemonic.
		for (String name : List.of("iload", "lload", "fload", "dload", "aload", "ret")) {
			Instruction<?> instruction = JvmInstructions.INSTANCE.get(name);
			assertNotNull(instruction, name);
			assertTrue(instruction.hasTrait(InstructionTrait.VARIABLE_READ), name);
			assertFalse(instruction.hasTrait(InstructionTrait.VARIABLE_WRITE), name);
			assertEquals(0, instruction.variableOperandIndex(), name);
		}

		for (String name : List.of("istore", "lstore", "fstore", "dstore", "astore")) {
			Instruction<?> instruction = JvmInstructions.INSTANCE.get(name);
			assertNotNull(instruction, name);
			assertTrue(instruction.hasTrait(InstructionTrait.VARIABLE_WRITE), name);
			assertFalse(instruction.hasTrait(InstructionTrait.VARIABLE_READ), name);
			assertEquals(0, instruction.variableOperandIndex(), name);
		}

		// iinc reads and writes the same local through one operand, which neither direction alone describes.
		Instruction<?> increment = JvmInstructions.INSTANCE.get("iinc");
		assertNotNull(increment, "iinc");
		assertTrue(increment.hasTrait(InstructionTrait.VARIABLE_INCREMENT), "iinc");
		assertEquals(0, increment.variableOperandIndex(), "iinc");
	}

	@Test
	void controlFlowTraitsDistinguishTheOutcomes() {
		// Each trait answers one question about control flow, and the answers cannot contradict.
		assertTraits("goto_w", InstructionTrait.UNCONDITIONAL_BRANCH);
		assertTraits("jsr_w", InstructionTrait.UNCONDITIONAL_BRANCH);
		assertTraits("ifeq", InstructionTrait.CONDITIONAL_BRANCH, InstructionTrait.FALLTHROUGH);
		assertTraits("tableswitch", InstructionTrait.SWITCH, InstructionTrait.PAYLOAD);
		assertTraits("lookupswitch", InstructionTrait.SWITCH, InstructionTrait.PAYLOAD);
		assertTraits("athrow", InstructionTrait.THROW);
		assertTraits("ireturn", InstructionTrait.RETURN);
		assertTraits("lreturn", InstructionTrait.RETURN);
		assertTraits("invokevirtual", InstructionTrait.INVOKE, InstructionTrait.METHOD_REFERENCE,
				InstructionTrait.FALLTHROUGH);
		assertTraits("getstatic", InstructionTrait.FIELD_REFERENCE, InstructionTrait.FALLTHROUGH);
		assertTraits("new", InstructionTrait.TYPE_REFERENCE, InstructionTrait.FALLTHROUGH);
		assertTraits("line", InstructionTrait.PSEUDO, InstructionTrait.DEBUG_METADATA);
	}

	private static void assertTraits(String name, InstructionTrait... traits) {
		Instruction<?> instruction = JvmInstructions.INSTANCE.get(name);
		assertNotNull(instruction, name);
		for (InstructionTrait trait : traits)
			assertTrue(instruction.hasTrait(trait), name + " should be " + trait);
		// A fall through is expected of everything that keeps executing, and of nothing that leaves the
		// flow; a pseudo-instruction carries no control-flow claim either way.
		if (instruction.hasTrait(InstructionTrait.PSEUDO))
			return;
		if (!List.of(traits).contains(InstructionTrait.UNCONDITIONAL_BRANCH)
				&& !List.of(traits).contains(InstructionTrait.RETURN)
				&& !List.of(traits).contains(InstructionTrait.THROW)
				&& !List.of(traits).contains(InstructionTrait.SWITCH))
			assertTrue(instruction.hasTrait(InstructionTrait.FALLTHROUGH), name + " should fall through");
		else
			assertFalse(instruction.hasTrait(InstructionTrait.FALLTHROUGH), name + " leaves the flow");
	}

	@Test
	void everyAliasIsRegisteredAndReachable() {
		// Aliases are declared by canonicalName alone, so the enumeration has to agree with the
		// registry rather than with a second list that could drift.
		for (String name : JvmInstructions.INSTANCE.getInstructionNames()) {
			Instruction<?> instruction = JvmInstructions.INSTANCE.get(name);
			assertNotNull(instruction, name);
			String canonical = instruction.canonicalName();
			for (String alias : JvmInstructions.INSTANCE.aliasesOf(canonical)) {
				assertNotNull(JvmInstructions.INSTANCE.get(alias), alias);
				assertTrue(JvmInstructions.INSTANCE.getSourceKeywords().contains(alias), alias);
				assertFalse(JvmInstructions.INSTANCE.aliasesOf(alias).contains(alias), alias);
			}
		}

		assertEquals(List.of("goto_w"), JvmInstructions.INSTANCE.aliasesOf("goto"));
		assertEquals(List.of("jsr_w"), JvmInstructions.INSTANCE.aliasesOf("jsr"));
		assertEquals(List.of("ldc2_w", "ldc_w"), JvmInstructions.INSTANCE.aliasesOf("ldc"));

		for (String canonical : JvmInstructions.INSTANCE.getCanonicalKeywords())
			assertNotNull(JvmInstructions.INSTANCE.get(canonical), canonical);
	}

	@Test
	void everyInstructionLowersThroughItsCanonicalMnemonicAndRoundTrips() {
		// Encoding-specific forms share their canonical opcode; source-name lookup also needs the
		// interface flag to distinguish method invocation forms that use the same opcode.
		for (String name : JvmInstructions.INSTANCE.getInstructionNames()) {
			Instruction<?> instruction = JvmInstructions.INSTANCE.get(name);
			assertNotNull(instruction, name);
			if (instruction.lowering() == null)
				continue; // Source-only forms such as `line` have no emitted opcode.

			assertDoesNotThrow(() -> JvmOpcodes.opcode(instruction.canonicalName()),
					name + " has no opcode under its canonical mnemonic");
			JvmLowering lowering = (JvmLowering) instruction.lowering();
			assertEquals(JvmOpcodes.opcode(instruction.canonicalName()), lowering.opcode(), name);
			assertEquals(instruction.canonicalName(), JvmInstructions.INSTANCE.getSourceName(
					lowering.opcode(), instruction.hasTrait(InstructionTrait.INTERFACE_INVOKE)), name);
		}
	}

	@Test
	void encodingFormAliasesCompileEndToEnd() {
		TestUtils.processJvm("""
				.super java/lang/Object
				.class public super Example {
				  .method public static wide ()J {
				    code: {
				      ldc2_w 1L
				      lreturn
				    }
				  }
				  .method public static narrow ()Ljava/lang/String; {
				    code: {
				      ldc_w "value"
				      areturn
				    }
				  }
				  .method public static jump ()V {
				    code: {
				      goto_w End
				    End:
				      return
				    }
				  }
			}
			""", new TestJvmCompilerOptions(), null);
	}

	/**
	 * @param kind
	 * 		Operand role kind.
	 *
	 * @return Reference policy the kind implies, so a role cannot state a contradictory pair.
	 */
	private static OperandRole.ReferencePolicy referencePolicyFor(OperandRole.RoleKind kind) {
		return switch (kind) {
			case LABEL -> OperandRole.ReferencePolicy.LABEL;
			case TYPE -> OperandRole.ReferencePolicy.TYPE;
			case SWITCH_PAYLOAD -> OperandRole.ReferencePolicy.PAYLOAD;
			case MEMBER -> OperandRole.ReferencePolicy.MEMBER;
			case VARIABLE -> OperandRole.ReferencePolicy.NONE;
		};
	}
}
