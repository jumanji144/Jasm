package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.instructions.JvmInstructions;
import me.darknet.assembler.backend.jvm.instructions.LookupSwitchPayload;
import me.darknet.assembler.backend.jvm.instructions.TableSwitchPayload;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.processing.ProcessedInstruction;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.backend.jvm.test.JvmAssemblerFixture;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that JVM operand schemas resolve typed semantic values during processing.
 * <p>
 * These assertions read values through {@link JvmInstructions} plus {@link SemanticProcessor} only,
 * so a regression that pushes member-path or switch-key parsing back into emission fails here rather
 * than surfacing as a raw exception in {@code JvmCodeVisitor}.
 */
class JvmSemanticOperandTest {

	@Test
	void resolvesFieldMemberPath() {
		ProcessedInstruction instruction = firstInstruction("""
				.method public static test ()V {
				  code: {
				    getstatic java/lang/System.out Ljava/io/PrintStream;
				    return
				  }
				}
				""");

		MemberPath path = instruction.operand(0, MemberPath.class);
		assertEquals("java/lang/System", path.owner());
		assertEquals("out", path.name());
	}

	@Test
	void resolvesMethodMemberPathForCallKinds() {
		ProcessedInstruction virtual = firstInstruction("""
				.method public static test ()V {
				  code: {
				    invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				    return
				  }
				}
				""");
		MemberPath virtualPath = virtual.operand(0, MemberPath.class);
		assertEquals("java/io/PrintStream", virtualPath.owner());
		assertEquals("println", virtualPath.name());

		ProcessedInstruction itf = firstInstruction("""
				.method public static test ()V {
				  code: {
				    invokeinterface java/util/List.size ()I
				    return
				  }
				}
				""");
		MemberPath itfPath = itf.operand(0, MemberPath.class);
		assertEquals("java/util/List", itfPath.owner());
		assertEquals("size", itfPath.name());

		// The interface flag lives on the registry trait, so the visitor never re-derives it from the
		// mnemonic; the emitted node proves it actually reached the code visitor.
		assertTrue(itf.definition().hasTrait(InstructionTrait.INTERFACE_INVOKE));
		MethodInsnNode emitted = emitMethodInsn("""
				.super java/lang/Object
				.class public super Example {
				  .method public static test (Ljava/util/List;)V {
				    code: {
				      aload 0
				      invokeinterface java/util/List.size ()I
				      pop
				      return
				    }
				  }
				}
				""");
		assertEquals("java/util/List", emitted.owner);
		assertEquals("size", emitted.name);
		assertEquals("()I", emitted.desc);
		assertTrue(emitted.itf, "invokeinterface must emit with the itf flag set");
	}

	@Test
	void resolvesLookupSwitchKeysInAnyRadix() {
		ProcessedInstruction instruction = firstInstruction("""
				.method public static test ()V {
				  code: {
				    lookupswitch { 0x10: A, -1: B, default: C }
				  A:
				    return
				  B:
				    return
				  C:
				    return
				  }
				}
				""");

		LookupSwitchPayload payload = instruction.operand(0, LookupSwitchPayload.class);
		assertEquals("C", payload.defaultLabel());
		// 0x10 proves radix handling moved to processing; -1 proves sign handling did too.
		assertEquals("A", payload.caseLabels().get(0x10));
		assertEquals("B", payload.caseLabels().get(-1));
	}

	@Test
	void resolvesTableSwitchBoundsAndCases() {
		ProcessedInstruction instruction = firstInstruction("""
				.method public static test ()V {
				  code: {
				    tableswitch { min: 3, max: 4, default: D, cases: { E, F } }
				  D:
				    return
				  E:
				    return
				  F:
				    return
				  }
				}
				""");

		TableSwitchPayload payload = instruction.operand(0, TableSwitchPayload.class);
		assertEquals(3, payload.min());
		assertEquals("D", payload.defaultLabel());
		assertEquals(List.of("E", "F"), payload.caseLabels());
	}

	@Test
	void reportsMalformedMemberPathAsDiagnostic() {
		// Previously this split "java/lang" at a missing '.' and threw StringIndexOutOfBoundsException
		// out of the code visitor.
		var parsed = AssemblyParseFixture.processDeclarations("<test>", """
				.method public static test ()V {
				  code: {
				    getstatic java/lang Ljava/io/PrintStream;
				    return
				  }
				}
				""", JvmTargetContext.INSTANCE);
		var processed = SemanticProcessor.process(
				DiagnosticAssertions.requireSuccess(parsed, "Source should parse"), JvmTargetContext.INSTANCE);
		DiagnosticAssertions.assertHasErrors(processed, "Malformed member path should fail processing");
		assertTrue(DiagnosticAssertions.formatErrors(processed.errors())
					.contains("Expected member path in owner.name form: java/lang"),
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
				AssemblyParseFixture.processDeclarations("<test>", source, JvmTargetContext.INSTANCE),
				"Source should parse");
		ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
				SemanticProcessor.process(ast, JvmTargetContext.INSTANCE), "Source should process semantically");
		ProcessedInstruction instruction = (ProcessedInstruction) unit.methods().values().iterator().next()
				.code().getFirst();
		assertNotNull(instruction, "Method body should carry a processed instruction");
		return instruction;
	}

	/**
	 * @param source
	 * 		JASM source to compile end to end.
	 *
	 * @return First method-invocation node of the emitted class.
	 */
	private static MethodInsnNode emitMethodInsn(String source) {
		byte[] bytes = JvmAssemblerFixture.compileJvm(source, new TestJvmCompilerOptions())
				.requireClassBytes();
		ClassNode node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		for (MethodNode method : node.methods) {
			for (AbstractInsnNode instruction : method.instructions) {
				if (instruction instanceof MethodInsnNode methodInsn)
					return methodInsn;
			}
		}
		throw new AssertionError("Compiled class contained no method invocation");
	}
}
