package me.darknet.assembler;

import me.darknet.assembler.printer.JvmPrinterUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Printer util tests. Basic coverage to ensure they include just the instruction
 * and not any surrounding context, since this is intended for quick debug printing.
 */
class JvmPrinterUtilTest {
	@Test
	void printsSimpleInstruction() {
		assertEquals("return", tos(new InsnNode(Opcodes.RETURN)));
	}

	@Test
	void printsLdcConstants() {
		assertEquals("ldc 42", tos(new LdcInsnNode(42)));
		assertEquals("ldc \"hello\"", tos(new LdcInsnNode("hello")));
		assertEquals("ldc Ljava/lang/String;", tos(new LdcInsnNode(Type.getType(String.class))));
	}

	@Test
	void printsJumpAndLabelWithStableNamesAcrossIterable() {
		LabelNode target = new LabelNode();

		assertEquals(
				"goto A\nA:",
				tos(List.of(new JumpInsnNode(Opcodes.GOTO, target), target))
		);
	}

	@Test
	void printsLookupSwitchWithConsistentLabelNames() {
		LabelNode caseA = new LabelNode();
		LabelNode caseB = new LabelNode();
		LabelNode defaultLabel = new LabelNode();

		assertEquals("""
						lookupswitch {
						10: A,
						20: B,
						default: C
						}
						A:
						B:
						C:
						""".stripTrailing(),
				tos(List.of(
						new LookupSwitchInsnNode(defaultLabel, new int[]{10, 20}, new LabelNode[]{caseA, caseB}),
						caseA,
						caseB,
						defaultLabel
				))
		);
	}

	@Test
	void printsTableSwitchWithConsistentLabelNames() {
		LabelNode caseA = new LabelNode();
		LabelNode caseB = new LabelNode();
		LabelNode defaultLabel = new LabelNode();

		assertEquals("""
						tableswitch {
						min: 3,
						max: 5,
						cases: { A, B },
						default: C
						}
						A:
						B:
						C:
						""".stripTrailing(),
				tos(List.of(
						new TableSwitchInsnNode(3, 4, defaultLabel, caseA, caseB),
						caseA,
						caseB,
						defaultLabel
				))
		);
	}

	@Test
	void printsInvokeDynamicAndConstantDynamic() {
		Handle bootstrap = new Handle(
				Opcodes.H_INVOKESTATIC,
				"java/lang/invoke/StringConcatFactory",
				"makeConcatWithConstants",
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
				false
		);
		ConstantDynamic dynamic = new ConstantDynamic("value", "Ljava/lang/String;", bootstrap, "prefix");

		assertEquals(
				"ldc { value, Ljava/lang/String;, { invokestatic, java/lang/invoke/StringConcatFactory.makeConcatWithConstants, (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; }, { \"prefix\" } }",
				tos(new LdcInsnNode(dynamic))
		);
		assertEquals(
				"invokedynamic concat (Ljava/lang/String;)Ljava/lang/String; { invokestatic, java/lang/invoke/StringConcatFactory.makeConcatWithConstants, (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; } { \"\\u0001\" }",
				tos(new InvokeDynamicInsnNode("concat", "(Ljava/lang/String;)Ljava/lang/String;", bootstrap, "\u0001"))
		);
	}

	@Test
	void skipsFrameNodes() {
		assertEquals("", tos(new FrameNode(Opcodes.F_SAME, 0, null, 0, null)));
	}

	@Test
	void iterablePrintingTrimsOnlyTrailingNewline() {
		assertEquals(
				"nop\naconst_null",
				tos(List.of(
						new InsnNode(Opcodes.NOP),
						new FrameNode(Opcodes.F_SAME, 0, null, 0, null),
						new InsnNode(Opcodes.ACONST_NULL)
				))
		);
	}

	private static String tos(AbstractInsnNode insn) {
		return JvmPrinterUtil.toString(insn);
	}

	private static String tos(Iterable<AbstractInsnNode> insns) {
		return JvmPrinterUtil.toString(insns);
	}
}
