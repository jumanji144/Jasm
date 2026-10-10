package me.darknet.assembler.backend.jvm.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.backend.jvm.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.SwitchShape;
import me.darknet.assembler.backend.jvm.util.JvmOpcodes;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers all JVM instructions with their respective operand types and visitors.
 */
public class JvmInstructions extends Instructions<ASTJvmInstructionVisitor> {
	public final static JvmInstructions INSTANCE = new JvmInstructions();

	private JvmInstructions() {
		super();
	}

	@Override
	protected void registerInstructions() {
		registerSimple(
				"nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4",
				"iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1",
				"iaload", "laload", "faload", "daload", "aaload", "baload", "caload", "saload", "iastore", "lastore",
				"fastore", "dastore", "aastore", "bastore", "castore", "sastore", "pop", "pop2", "dup", "dup_x1",
				"dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub",
				"dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem",
				"ineg", "lneg", "fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land", "ior",
				"lor", "ixor", "lxor", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l",
				"d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "arraylength",
				"monitorenter", "monitorexit"
		);
		registerSimpleTraits(EnumSet.of(InstructionTrait.RETURN), "ireturn", "lreturn", "freturn", "dreturn", "areturn", "return");
		registerSimpleTraits(EnumSet.of(InstructionTrait.THROW), "athrow");
		registerIntProcessors("bipush", "sipush");
		registerTypeProcessors(JvmOperands.CLASS_TYPE, "new"); // new accepts class types, but not array types.
		registerTypeProcessors(JvmOperands.TYPE, "anewarray", "checkcast", "instanceof");

		register("newarray", ops(JvmOperands.NEW_ARRAY_TYPE),
				(inst, visitor) -> visitor.visitNewArrayInsn(inst.argument(0, ASTIdentifier.class)),
				metadata(EnumSet.of(InstructionTrait.FALLTHROUGH), List.of(), null, "newarray",
						loweringFor("newarray", "newarray"))
		);
		register("ldc", ops(JvmOperands.CONSTANT),
				(inst, visitor) -> visitor.visitLdcInsn(inst.argument(0, ASTElement.class)),
				metadata(EnumSet.of(InstructionTrait.FALLTHROUGH), List.of(), null, "ldc", loweringFor("ldc", "ldc"))
		);
		registerVarProcessors(
				"iload", "lload", "fload", "dload", "aload", "istore", "lstore", "fstore", "dstore", "astore", "ret"
		);
		register("iinc", ops(DefaultOperands.VARIABLE_NAME, DefaultOperands.INTEGER),
				(inst, visitor) -> visitor.visitIincInsn(inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTNumber.class)),
				metadata(EnumSet.of(InstructionTrait.VARIABLE_INCREMENT, InstructionTrait.FALLTHROUGH),
						List.of(new OperandRole(0, OperandRole.RoleKind.VARIABLE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.NONE)), null, "iinc",
						loweringFor("iinc", "iinc"))
		);
		registerConditionalJumps(
				"ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle",
				"if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge",
				"if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne",
				"ifnull", "ifnonnull"
		);
		registerUnconditionalJumps("goto", "jsr");
		register("tableswitch", ops(JvmOperands.TABLE_SWITCH),
				(inst, visitor) -> visitor.visitTableSwitchInsn(inst.operand(0, TableSwitchPayload.class)),
				metadata(EnumSet.of(InstructionTrait.SWITCH, InstructionTrait.PAYLOAD),
						List.of(new OperandRole(0, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.TABLE, "tableswitch",
						loweringFor("tableswitch", "tableswitch"))
		);
		register("lookupswitch", ops(JvmOperands.LOOKUP_SWITCH),
				(inst, visitor) -> visitor.visitLookupSwitchInsn(inst.operand(0, LookupSwitchPayload.class)),
				metadata(EnumSet.of(InstructionTrait.SWITCH, InstructionTrait.PAYLOAD),
						List.of(new OperandRole(0, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.LOOKUP, "lookupswitch",
						loweringFor("lookupswitch", "lookupswitch"))
		);
		registerFieldProcessors("getstatic", "putstatic", "getfield", "putfield");
		registerMethodProcessors(
				"invokevirtual", "invokespecial", "invokestatic", "invokeinterface",

				// Our very special itf=true flag alias instructions. These are not real instructions but alias to the
				// canonical forms with the interface flag set. Doing it this way is less cringeworthy than having an
				// alterantive invoke form that specifies the itf=true flag. These are basically never used in real code.
				"invokevirtualinterface", "invokestaticinterface", "invokespecialinterface"
		);

		register("invokedynamic", ops(DefaultOperands.LITERAL, DefaultOperands.DESCRIPTOR, JvmOperands.HANDLE, JvmOperands.ARGS),
				(inst, visitor) -> visitor.visitInvokeDynamicInsn(
						// Operand 2 is the bootstrap handle and operand 3 is the argument array. The handle
						// is read as the element the visitor declares, because its accepted spellings are
						// resolved by the operand schema rather than by this read.
						inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTIdentifier.class),
						inst.argument(2, ASTElement.class), inst.argument(3, ASTArray.class)
				),
				metadata(EnumSet.of(InstructionTrait.INVOKE, InstructionTrait.FALLTHROUGH), List.of(), null,
						"invokedynamic", loweringFor("invokedynamic", "invokedynamic"))
		);

		register("multianewarray", ops(JvmOperands.TYPE, DefaultOperands.INTEGER),
				(inst, visitor) -> visitor.visitMultiANewArrayInsn(inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTNumber.class)),
				metadata(EnumSet.of(InstructionTrait.TYPE_REFERENCE, InstructionTrait.PAYLOAD),
						List.of(new OperandRole(0, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "multianewarray",
						loweringFor("multianewarray", "multianewarray"))
		);
		register("line", ops(DefaultOperands.INTEGER),
				(inst, visitor) -> visitor.visitLineNumber(inst.argument(0, ASTNumber.class)),
				sourceMetadata(EnumSet.of(InstructionTrait.PSEUDO, InstructionTrait.DEBUG_METADATA), List.of(), null, "line")
		);

		// We normally canonicalize the wide forms to their non-wide counterparts, but if the user explicitly writes
		// then we will accept them as-is and lower them to the canonical form.
		register("jsr_w", ops(DefaultOperands.LABEL),
				(inst, visitor) -> visitor.visitJumpInsn(inst.argument(0, ASTIdentifier.class)),
				metadata(EnumSet.of(InstructionTrait.UNCONDITIONAL_BRANCH),
						List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, "jsr",
						loweringFor("jsr_w", "jsr"))
		);
		register("goto_w", ops(DefaultOperands.LABEL),
				(inst, visitor) -> visitor.visitJumpInsn(inst.argument(0, ASTIdentifier.class)),
				metadata(EnumSet.of(InstructionTrait.UNCONDITIONAL_BRANCH),
						List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, "goto",
						loweringFor("goto_w", "goto"))
		);
		register("ldc_w", ops(JvmOperands.CONSTANT),
				(inst, visitor) -> visitor.visitLdcInsn(inst.argument(0, ASTElement.class)),
				metadata(EnumSet.of(InstructionTrait.FALLTHROUGH), List.of(), null, "ldc", loweringFor("ldc_w", "ldc"))
		);
		register("ldc2_w", ops(JvmOperands.WIDE_CONSTANT),
				(inst, visitor) -> visitor.visitLdcInsn(inst.argument(0, ASTNumber.class)),
				metadata(EnumSet.of(InstructionTrait.FALLTHROUGH), List.of(), null, "ldc", loweringFor("ldc2_w", "ldc"))
		);
	}

	/**
	 * Registers a set of instructions that have no operands and fall through to the next instruction.
	 *
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerSimple(String... names) {
		registerSimpleTraits(EnumSet.of(InstructionTrait.FALLTHROUGH), names);
	}

	/**
	 * Registers a set of instructions that have no operands and fall through to the next instruction.
	 *
	 * @param traits
	 * 		Traits carried by every registerable name.
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerSimpleTraits(EnumSet<InstructionTrait> traits, String... names) {
		for (String name : names) {
			register(name, ops(), (inst, visitor) -> visitor.visitInsn(),
					metadata(traits, List.of(), null, name, loweringFor(name, name)));
		}
	}

	/**
	 * Registers a set of instructions that have a single integer operand and fall through to the next instruction.
	 *
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerIntProcessors(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.INTEGER),
					(inst, visitor) -> visitor.visitIntInsn(inst.argument(0, ASTNumber.class)),
					metadata(EnumSet.of(InstructionTrait.FALLTHROUGH), List.of(), null, name, loweringFor(name, name))
			);
		}
	}

	/**
	 * Registers a set of instructions that have a single variable operand and fall through to the next instruction.
	 *
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerVarProcessors(String... names) {
		for (String name : names) {
			InstructionTrait access = name.endsWith("store")
					? InstructionTrait.VARIABLE_WRITE
					: InstructionTrait.VARIABLE_READ;
			EnumSet<InstructionTrait> traits = EnumSet.of(InstructionTrait.FALLTHROUGH, access);
			register(name, ops(DefaultOperands.VARIABLE_NAME),
					(inst, visitor) -> visitor.visitVarInsn(inst.argument(0, ASTIdentifier.class)),
					metadata(traits, List.of(new OperandRole(0, OperandRole.RoleKind.VARIABLE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.NONE)), null, name,
							loweringFor(name, name))
			);
		}
	}

	/**
	 * Registers a set of instructions that have a single label operand and fall through to the next instruction.
	 *
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerConditionalJumps(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.LABEL),
					(inst, visitor) -> visitor.visitJumpInsn(inst.argument(0, ASTIdentifier.class)),
					metadata(EnumSet.of(InstructionTrait.CONDITIONAL_BRANCH, InstructionTrait.FALLTHROUGH),
							List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, name, loweringFor(name, name))
			);
		}
	}

	/**
	 * Registers a set of instructions that have a single label operand and do not fall through to the next instruction.
	 *
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerUnconditionalJumps(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.LABEL),
					(inst, visitor) -> visitor.visitJumpInsn(inst.argument(0, ASTIdentifier.class)),
					metadata(EnumSet.of(InstructionTrait.UNCONDITIONAL_BRANCH),
							List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, name, loweringFor(name, name))
			);
		}
	}

	/**
	 * Registers a set of instructions that have a member path and field descriptor operands and fall through to the next instruction.
	 *
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerFieldProcessors(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.MEMBER_PATH, DefaultOperands.FIELD_DESCRIPTOR),
					(inst, visitor) -> visitor.visitFieldInsn(inst.operand(0, MemberPath.class), inst.argument(1, ASTIdentifier.class)),
					metadata(EnumSet.of(InstructionTrait.FALLTHROUGH, InstructionTrait.FIELD_REFERENCE),
							List.of(new OperandRole(0, OperandRole.RoleKind.MEMBER, OperandRole.WidthPolicy.SINGLE,
									OperandRole.ReferencePolicy.MEMBER)), null, name, loweringFor(name, name))
			);
		}
	}

	/**
	 * Registers a set of instructions that have a member path and method descriptor operands and fall through to the next instruction.
	 *
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerMethodProcessors(String... names) {
		EnumSet<InstructionTrait> traits = EnumSet.of(InstructionTrait.INVOKE, InstructionTrait.METHOD_REFERENCE, InstructionTrait.FALLTHROUGH);
		for (String name : names) {
			boolean itf = name.endsWith("interface");
			if (itf)
				traits.add(InstructionTrait.INTERFACE_INVOKE);
			register(name, ops(DefaultOperands.MEMBER_PATH, DefaultOperands.METHOD_DESCRIPTOR),
					(inst, visitor) -> visitor.visitMethodInsn(inst.operand(0, MemberPath.class), inst.argument(1, ASTIdentifier.class), itf),
					metadata(traits, List.of(new OperandRole(0, OperandRole.RoleKind.MEMBER,
									OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.MEMBER)), null, name,
							loweringFor(name, name))
			);
		}
	}

	/**
	 * Registers a set of instructions that have a single type operand and fall through to the next instruction.
	 *
	 * @param operand
	 * 		Operand type accepted by the instruction.
	 * @param names
	 * 		Registered source mnemonics.
	 */
	void registerTypeProcessors(JvmOperands operand, String... names) {
		for (String name : names) {
			register(name, ops(operand), (inst, visitor) -> visitor.visitTypeInsn(inst.argument(0, ASTIdentifier.class)),
					metadata(EnumSet.of(InstructionTrait.TYPE_REFERENCE, InstructionTrait.FALLTHROUGH),
							List.of(new OperandRole(0, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, name, loweringFor(name, name)));
		}
	}

	/**
	 * Looks up the registered source mnemonic for an emitted opcode.
	 *
	 * @param opcode
	 * 		ASM opcode of the emitted node.
	 *
	 * @return Registered source mnemonic, or {@code null} when no instruction matches.
	 */
	public @Nullable String getSourceName(int opcode) {
		return getSourceName(opcode, false);
	}

	/**
	 * Looks up the registered source mnemonic for an emitted opcode and its interface flag.
	 *
	 * @param opcode
	 * 		ASM opcode of the emitted node.
	 * @param itf
	 * 		Whether the node dispatches through an interface.
	 *
	 * @return Registered source mnemonic, or {@code null} when no instruction matches.
	 */
	public @Nullable String getSourceName(int opcode, boolean itf) {
		return SourceNames.byOpcode.get(new SourceKey(opcode, itf));
	}

	/**
	 * Maps an emitted opcode and its interface flag back to the JASM source spelling.
	 */
	private static final class SourceNames {
		private static final Map<SourceKey, String> byOpcode = build();

		private static Map<SourceKey, String> build() {
			Map<SourceKey, String> names = new HashMap<>();
			for (String name : INSTANCE.getInstructionNames().stream().sorted().toList()) {
				Instruction<ASTJvmInstructionVisitor> instruction = INSTANCE.get(name);
				// Source-only forms such as 'line' have no emitted opcode, so they cannot be a spelling.
				if (instruction == null || !(instruction.lowering() instanceof JvmLowering(int opcode)))
					continue;
				names.putIfAbsent(new SourceKey(opcode, instruction.hasTrait(InstructionTrait.INTERFACE_INVOKE)), name);
			}
			return Map.copyOf(names);
		}
	}

	/**
	 * Emitted opcode plus interface flag, which together identify one source spelling.
	 *
	 * @param opcode
	 * 		ASM opcode of the emitted node.
	 * @param itf
	 * 		Whether the node dispatches through an interface.
	 */
	private record SourceKey(int opcode, boolean itf) {}

	/**
	 * @param name
	 * 		Registered source mnemonic.
	 * @param canonicalName
	 * 		Canonical mnemonic whose opcode this form encodes as.
	 *
	 * @return Lowering identity for {@code name}.
	 *
	 * @throws IllegalStateException
	 * 		If {@code canonicalName} has no ASM opcode, so a typo fails at registry construction rather than at emission.
	 */
	private static JvmLowering loweringFor(String name, String canonicalName) {
		return new JvmLowering(JvmOpcodes.opcode(canonicalName));
	}
}
