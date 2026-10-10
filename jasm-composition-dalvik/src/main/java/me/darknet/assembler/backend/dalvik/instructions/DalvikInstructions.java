package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.backend.dalvik.visitor.ASTDalvikInstructionVisitor;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.InstructionMetadata;
import me.darknet.assembler.instructions.InstructionTrait;
import static me.darknet.assembler.instructions.InstructionTrait.*;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.Operands;
import me.darknet.assembler.instructions.SwitchShape;
import me.darknet.dex.file.instructions.Opcodes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers all Dalvik instructions with their respective operand types and visitors.
 * <p>
 * The opcode tables below are the registration lists: each mnemonic is registered with the dex
 * opcode it encodes as, so the code visitor never has to rediscover a family from mnemonic text.
 */
public class DalvikInstructions extends Instructions<ASTDalvikInstructionVisitor> implements Opcodes {
	private static final Map<String, Integer> BINARY_OPCODES = Map.ofEntries(
			Map.entry("add-int", ADD_INT),
			Map.entry("sub-int", SUB_INT),
			Map.entry("mul-int", MUL_INT),
			Map.entry("div-int", DIV_INT),
			Map.entry("rem-int", REM_INT),
			Map.entry("and-int", AND_INT),
			Map.entry("or-int", OR_INT),
			Map.entry("xor-int", XOR_INT),
			Map.entry("shl-int", SHL_INT),
			Map.entry("shr-int", SHR_INT),
			//
			Map.entry("ushr-int", USHR_INT),
			Map.entry("add-long", ADD_LONG),
			Map.entry("sub-long", SUB_LONG),
			Map.entry("mul-long", MUL_LONG),
			Map.entry("div-long", DIV_LONG),
			Map.entry("rem-long", REM_LONG),
			Map.entry("and-long", AND_LONG),
			Map.entry("or-long", OR_LONG),
			Map.entry("xor-long", XOR_LONG),
			Map.entry("shl-long", SHL_LONG),
			Map.entry("shr-long", SHR_LONG),
			Map.entry("ushr-long", USHR_LONG),
			//
			Map.entry("add-float", ADD_FLOAT),
			Map.entry("sub-float", SUB_FLOAT),
			Map.entry("mul-float", MUL_FLOAT),
			Map.entry("div-float", DIV_FLOAT),
			Map.entry("rem-float", REM_FLOAT),
			//
			Map.entry("add-double", ADD_DOUBLE),
			Map.entry("sub-double", SUB_DOUBLE),
			Map.entry("mul-double", MUL_DOUBLE),
			Map.entry("div-double", DIV_DOUBLE),
			Map.entry("rem-double", REM_DOUBLE)
	);

	private static final Map<String, Integer> BINARY_2ADDR_OPCODES = Map.ofEntries(
			Map.entry("add-int/2addr", ADD_INT_2ADDR),
			Map.entry("sub-int/2addr", SUB_INT_2ADDR),
			Map.entry("mul-int/2addr", MUL_INT_2ADDR),
			Map.entry("div-int/2addr", DIV_INT_2ADDR),
			Map.entry("rem-int/2addr", REM_INT_2ADDR),
			Map.entry("and-int/2addr", AND_INT_2ADDR),
			Map.entry("or-int/2addr", OR_INT_2ADDR),
			Map.entry("xor-int/2addr", XOR_INT_2ADDR),
			Map.entry("shl-int/2addr", SHL_INT_2ADDR),
			Map.entry("shr-int/2addr", SHR_INT_2ADDR),
			Map.entry("ushr-int/2addr", USHR_INT_2ADDR),
			//
			Map.entry("add-long/2addr", ADD_LONG_2ADDR),
			Map.entry("sub-long/2addr", SUB_LONG_2ADDR),
			Map.entry("mul-long/2addr", MUL_LONG_2ADDR),
			Map.entry("div-long/2addr", DIV_LONG_2ADDR),
			Map.entry("rem-long/2addr", REM_LONG_2ADDR),
			Map.entry("and-long/2addr", AND_LONG_2ADDR),
			Map.entry("or-long/2addr", OR_LONG_2ADDR),
			Map.entry("xor-long/2addr", XOR_LONG_2ADDR),
			Map.entry("shl-long/2addr", SHL_LONG_2ADDR),
			Map.entry("shr-long/2addr", SHR_LONG_2ADDR),
			Map.entry("ushr-long/2addr", USHR_LONG_2ADDR),
			//
			Map.entry("add-float/2addr", ADD_FLOAT_2ADDR),
			Map.entry("sub-float/2addr", SUB_FLOAT_2ADDR),
			Map.entry("mul-float/2addr", MUL_FLOAT_2ADDR),
			Map.entry("div-float/2addr", DIV_FLOAT_2ADDR),
			Map.entry("rem-float/2addr", REM_FLOAT_2ADDR),
			//
			Map.entry("add-double/2addr", ADD_DOUBLE_2ADDR),
			Map.entry("sub-double/2addr", SUB_DOUBLE_2ADDR),
			Map.entry("mul-double/2addr", MUL_DOUBLE_2ADDR),
			Map.entry("div-double/2addr", DIV_DOUBLE_2ADDR),
			Map.entry("rem-double/2addr", REM_DOUBLE_2ADDR)
	);

	private static final Map<String, Integer> BINARY_LITERAL_16_OPCODES = Map.ofEntries(
			Map.entry("add-int/lit16", ADD_INT_LIT16),
			Map.entry("mul-int/lit16", MUL_INT_LIT16),
			Map.entry("div-int/lit16", DIV_INT_LIT16),
			Map.entry("rem-int/lit16", REM_INT_LIT16),
			Map.entry("and-int/lit16", AND_INT_LIT16),
			Map.entry("or-int/lit16", OR_INT_LIT16),
			Map.entry("xor-int/lit16", XOR_INT_LIT16),
			Map.entry("rsub-int", RSUB_INT)
	);

	private static final Map<String, Integer> BINARY_LITERAL_8_OPCODES = Map.ofEntries(
			Map.entry("add-int/lit8", ADD_INT_LIT8),
			Map.entry("rsub-int/lit8", RSUB_INT_LIT8),
			Map.entry("mul-int/lit8", MUL_INT_LIT8),
			Map.entry("div-int/lit8", DIV_INT_LIT8),
			Map.entry("rem-int/lit8", REM_INT_LIT8),
			Map.entry("and-int/lit8", AND_INT_LIT8),
			Map.entry("or-int/lit8", OR_INT_LIT8),
			Map.entry("xor-int/lit8", XOR_INT_LIT8),
			Map.entry("shl-int/lit8", SHL_INT_LIT8),
			Map.entry("shr-int/lit8", SHR_INT_LIT8),
			Map.entry("ushr-int/lit8", USHR_INT_LIT8)
	);

	private static final Map<String, Integer> COMPARE_OPCODES = Map.ofEntries(
			Map.entry("cmpl-float", CMPL_FLOAT),
			Map.entry("cmpg-float", CMPG_FLOAT),
			Map.entry("cmpl-double", CMPL_DOUBLE),
			Map.entry("cmpg-double", CMPG_DOUBLE),
			Map.entry("cmp-long", CMP_LONG)
	);

	private static final Map<String, Integer> IF_OPCODES = Map.ofEntries(
			Map.entry("if-eq", IF_EQ),
			Map.entry("if-ne", IF_NE),
			Map.entry("if-lt", IF_LT),
			Map.entry("if-ge", IF_GE),
			Map.entry("if-gt", IF_GT),
			Map.entry("if-le", IF_LE)
	);

	private static final Map<String, Integer> IF_ZERO_OPCODES = Map.ofEntries(
			Map.entry("if-eqz", IF_EQZ),
			Map.entry("if-nez", IF_NEZ),
			Map.entry("if-ltz", IF_LTZ),
			Map.entry("if-gez", IF_GEZ),
			Map.entry("if-gtz", IF_GTZ),
			Map.entry("if-lez", IF_LEZ)
	);

	private static final Map<String, Integer> ARRAY_OPCODES = Map.ofEntries(
			Map.entry("aget", AGET),
			Map.entry("aget-wide", AGET_WIDE),
			Map.entry("aget-object", AGET_OBJECT),
			Map.entry("aget-boolean", AGET_BOOLEAN),
			Map.entry("aget-byte", AGET_BYTE),
			Map.entry("aget-char", AGET_CHAR),
			Map.entry("aget-short", AGET_SHORT),
			Map.entry("aput", APUT),
			Map.entry("aput-wide", APUT_WIDE),
			Map.entry("aput-object", APUT_OBJECT),
			Map.entry("aput-boolean", APUT_BOOLEAN),
			Map.entry("aput-byte", APUT_BYTE),
			Map.entry("aput-char", APUT_CHAR),
			Map.entry("aput-short", APUT_SHORT)
	);

	private static final Map<String, Integer> INSTANCE_FIELD_OPCODES = Map.ofEntries(
			Map.entry("iget", IGET),
			Map.entry("iget-wide", IGET_WIDE),
			Map.entry("iget-object", IGET_OBJECT),
			Map.entry("iget-boolean", IGET_BOOLEAN),
			Map.entry("iget-byte", IGET_BYTE),
			Map.entry("iget-char", IGET_CHAR),
			Map.entry("iget-short", IGET_SHORT),
			Map.entry("iput", IPUT),
			Map.entry("iput-wide", IPUT_WIDE),
			Map.entry("iput-object", IPUT_OBJECT),
			Map.entry("iput-boolean", IPUT_BOOLEAN),
			Map.entry("iput-byte", IPUT_BYTE),
			Map.entry("iput-char", IPUT_CHAR),
			Map.entry("iput-short", IPUT_SHORT)
	);

	private static final Map<String, Integer> STATIC_FIELD_OPCODES = Map.ofEntries(
			Map.entry("sget", SGET),
			Map.entry("sget-wide", SGET_WIDE),
			Map.entry("sget-object", SGET_OBJECT),
			Map.entry("sget-boolean", SGET_BOOLEAN),
			Map.entry("sget-byte", SGET_BYTE),
			Map.entry("sget-char", SGET_CHAR),
			Map.entry("sget-short", SGET_SHORT),
			Map.entry("sput", SPUT),
			Map.entry("sput-wide", SPUT_WIDE),
			Map.entry("sput-object", SPUT_OBJECT),
			Map.entry("sput-boolean", SPUT_BOOLEAN),
			Map.entry("sput-byte", SPUT_BYTE),
			Map.entry("sput-char", SPUT_CHAR),
			Map.entry("sput-short", SPUT_SHORT)
	);

	private static final Map<String, Integer> INVOKE_OPCODES = Map.ofEntries(
			Map.entry("invoke-virtual", INVOKE_VIRTUAL),
			Map.entry("invoke-super", INVOKE_SUPER),
			Map.entry("invoke-direct", INVOKE_DIRECT),
			Map.entry("invoke-static", INVOKE_STATIC),
			Map.entry("invoke-interface", INVOKE_INTERFACE),
			Map.entry("invoke-virtual/range", INVOKE_VIRTUAL_RANGE),
			Map.entry("invoke-super/range", INVOKE_SUPER_RANGE),
			Map.entry("invoke-direct/range", INVOKE_DIRECT_RANGE),
			Map.entry("invoke-static/range", INVOKE_STATIC_RANGE),
			Map.entry("invoke-interface/range", INVOKE_INTERFACE_RANGE)
	);

	private static final Map<String, Integer> UNARY_OPCODES = Map.ofEntries(
			Map.entry("neg-int", NEG_INT),
			Map.entry("not-int", NOT_INT),
			Map.entry("neg-long", NEG_LONG),
			Map.entry("not-long", NOT_LONG),
			Map.entry("neg-float", NEG_FLOAT),
			Map.entry("neg-double", NEG_DOUBLE),
			Map.entry("int-to-long", INT_TO_LONG),
			Map.entry("int-to-float", INT_TO_FLOAT),
			Map.entry("int-to-double", INT_TO_DOUBLE),
			Map.entry("long-to-int", LONG_TO_INT),
			Map.entry("long-to-float", LONG_TO_FLOAT),
			Map.entry("long-to-double", LONG_TO_DOUBLE),
			Map.entry("float-to-int", FLOAT_TO_INT),
			Map.entry("float-to-long", FLOAT_TO_LONG),
			Map.entry("float-to-double", FLOAT_TO_DOUBLE),
			Map.entry("double-to-int", DOUBLE_TO_INT),
			Map.entry("double-to-long", DOUBLE_TO_LONG),
			Map.entry("double-to-float", DOUBLE_TO_FLOAT),
			Map.entry("int-to-byte", INT_TO_BYTE),
			Map.entry("int-to-char", INT_TO_CHAR),
			Map.entry("int-to-short", INT_TO_SHORT)
	);

	/**
	 * Operand positions holding a wide value, keyed by mnemonic, for the unary and conversion forms.
	 * <p>
	 * Width is declared per operand rather than inferred from the mnemonic, because the same mnemonic can be used
	 * for both narrow and wide forms ({@code int-to-long} vs {@code int-to-float}).
	 */
	private static final Map<String, int[]> UNARY_WIDE_OPERANDS = Map.ofEntries(
			Map.entry("neg-long", new int[]{0, 1}),
			Map.entry("not-long", new int[]{0, 1}),
			Map.entry("neg-double", new int[]{0, 1}),
			// The destination is a long or double, so the source is the narrow type being widened.
			Map.entry("int-to-long", new int[]{0}),
			Map.entry("int-to-double", new int[]{0}),
			Map.entry("float-to-long", new int[]{0}),
			Map.entry("float-to-double", new int[]{0}),
			// The source is a long or double, so the destination is the narrow type being narrowed to.
			Map.entry("long-to-int", new int[]{1}),
			Map.entry("long-to-float", new int[]{1}),
			Map.entry("double-to-int", new int[]{1}),
			Map.entry("double-to-float", new int[]{1}),
			// Both sides are wide.
			Map.entry("long-to-double", new int[]{0, 1}),
			Map.entry("double-to-long", new int[]{0, 1})
	);

	/**
	 * Array operations whose register operand holds a wide value.
	 * The destination of a wide load and the value of a wide store, both at operand 0.
	 * The array and index operands are narrow.
	 */
	private static final Set<String> WIDE_ARRAY_OPERANDS = Set.of("aget-wide", "aput-wide");

	/**
	 * Field operations whose register operand holds a wide value.
	 * The destination of a wide static or instance load and the value of a wide store, all at operand 0.
	 */
	private static final Set<String> WIDE_FIELD_OPERANDS = Set.of("iget-wide", "iput-wide", "sget-wide", "sput-wide");

	/** Falls through to the next instruction without touching a register. */
	private static final Set<InstructionTrait> FT = Set.of(FALLTHROUGH);
	/** Reads a register and falls through. */
	private static final Set<InstructionTrait> READ_FT = Set.of(FALLTHROUGH, REGISTER_READ);
	/** Writes a register and falls through. */
	private static final Set<InstructionTrait> WRITE_FT = Set.of(FALLTHROUGH, REGISTER_WRITE);
	/** Reads and writes registers and falls through, which covers every register-machine operation. */
	private static final Set<InstructionTrait> READ_WRITE_FT = Set.of(FALLTHROUGH, REGISTER_READ, REGISTER_WRITE);
	/** Leaves the method with a value, so it reads the returned register instead of falling through. */
	private static final Set<InstructionTrait> RETURN_TRAIT = Set.of(InstructionTrait.RETURN, REGISTER_READ);
	/** Transfers control to a handler instead of falling through. */
	private static final Set<InstructionTrait> THROW_TRAIT = Set.of(InstructionTrait.THROW, REGISTER_READ);
	/** Dispatches to a method referenced by a member path and falls through after. */
	private static final Set<InstructionTrait> INVOKE_TRAIT = Set.of(FALLTHROUGH, INVOKE, METHOD_REFERENCE, REGISTER_READ);
	/**
	 * Dispatches through a bootstrap method handle.
	 * The handle is a constant, not a member path, so this form declares no member role and makes no member-reference claim.
	 */
	private static final Set<InstructionTrait> INVOKE_CUSTOM_TRAIT = Set.of(FALLTHROUGH, INVOKE, REGISTER_READ);

	public static final DalvikInstructions INSTANCE = new DalvikInstructions();

	@Override
	protected void registerInstructions() {
		register("nop", ops(), (inst, visitor) -> visitor.visitNop(), lowering(NOP, "nop", FT));
		register("move", ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitMove(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class)),
				lowering(MOVE, "move", READ_WRITE_FT));
		register("move-wide", ops(registerOperands(2, 0, 1)),
				(inst, visitor) -> visitor.visitMove(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class)),
				lowering(MOVE_WIDE, "move-wide", READ_WRITE_FT, wideRegisters(0, 1)));
		register("move-object", ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitMove(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class)),
				lowering(MOVE_OBJECT, "move-object", READ_WRITE_FT));
		register("move-result", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitMoveResult(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class)),
				lowering(MOVE_RESULT, "move-result", WRITE_FT));
		register("move-result-wide", ops(DalvikOperands.REGISTER_WIDE),
				(inst, visitor) -> visitor.visitMoveResult(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class)),
				lowering(MOVE_RESULT_WIDE, "move-result-wide", WRITE_FT, wideRegisters(0)));
		register("move-result-object", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitMoveResult(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class)),
				lowering(MOVE_RESULT_OBJECT, "move-result-object", WRITE_FT));
		register("move-exception", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitMoveException(inst.operand(0, RegisterRef.class)),
				lowering(MOVE_EXCEPTION, "move-exception", WRITE_FT));
		register("return-void", ops(), (inst, visitor) -> visitor.visitReturnVoid(), lowering(RETURN_VOID, "return-void", RETURN_TRAIT));
		register("return", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitReturn(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class)),
				lowering(RETURN, "return", RETURN_TRAIT));
		register("return-wide", ops(DalvikOperands.REGISTER_WIDE),
				(inst, visitor) -> visitor.visitReturn(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class)),
				lowering(RETURN_WIDE, "return-wide", RETURN_TRAIT, wideRegisters(0)));
		register("return-object", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitReturn(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class)),
				lowering(RETURN_OBJECT, "return-object", RETURN_TRAIT));
		register("const", ops(DalvikOperands.REGISTER, DefaultOperands.NUMBER),
				(inst, visitor) -> visitor.visitConst(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.argument(1, ASTElement.class)),
				lowering(CONST, "const", WRITE_FT));
		register("const-wide", ops(DalvikOperands.REGISTER_WIDE, DefaultOperands.NUMBER),
				(inst, visitor) -> visitor.visitConst(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.argument(1, ASTElement.class)),
				lowering(CONST_WIDE, "const-wide", WRITE_FT, wideRegisters(0)));
		register("const-string", ops(DalvikOperands.REGISTER, DefaultOperands.STRING),
				(inst, visitor) -> visitor.visitConst(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.argument(1, ASTElement.class)),
				lowering(CONST_STRING, "const-string", WRITE_FT));
		register("const-class", ops(DalvikOperands.REGISTER, DalvikOperands.CLASS_TYPE),
				(inst, visitor) -> visitor.visitConst(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.argument(1, ASTElement.class)),
				metadata(EnumSet.of(TYPE_REFERENCE, REGISTER_WRITE, FALLTHROUGH),
						List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "const-class",
						new DalvikLowering(CONST_CLASS)));
		register("const-method-handle", ops(DalvikOperands.REGISTER, DalvikOperands.HANDLE),
				(inst, visitor) -> visitor.visitConst(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.argument(1, ASTElement.class)),
				lowering(CONST_METHOD_HANDLE, "const-method-handle", WRITE_FT));
		register("const-method-type", ops(DalvikOperands.REGISTER, DalvikOperands.METHOD_TYPE),
				(inst, visitor) -> visitor.visitConst(inst.lowering(DalvikLowering.class), inst.operand(0, RegisterRef.class), inst.argument(1, ASTElement.class)),
				lowering(CONST_METHOD_TYPE, "const-method-type", WRITE_FT));
		register("monitor-enter", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitMonitorEnter(inst.operand(0, RegisterRef.class)),
				lowering(MONITOR_ENTER, "monitor-enter", READ_FT));
		register("monitor-exit", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitMonitorExit(inst.operand(0, RegisterRef.class)),
				lowering(MONITOR_EXIT, "monitor-exit", READ_FT));
		register("check-cast", ops(DalvikOperands.REGISTER, DalvikOperands.CLASS_TYPE),
				(inst, visitor) -> visitor.visitCheckCast(inst.operand(0, RegisterRef.class), inst.argument(1, ASTIdentifier.class)),
				metadata(EnumSet.of(TYPE_REFERENCE, REGISTER_READ, REGISTER_WRITE, FALLTHROUGH),
						List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "check-cast",
						new DalvikLowering(CHECK_CAST)));
		register("instance-of", ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER, DalvikOperands.CLASS_TYPE),
				(inst, visitor) -> visitor.visitInstanceOf(inst.operand(0, RegisterRef.class),
						inst.operand(1, RegisterRef.class), inst.argument(2, ASTIdentifier.class)),
				metadata(EnumSet.of(TYPE_REFERENCE, REGISTER_READ, REGISTER_WRITE, FALLTHROUGH),
						List.of(new OperandRole(2, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "instance-of",
						new DalvikLowering(INSTANCE_OF)));
		register("array-length", ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitArrayLength(inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class)),
				lowering(ARRAY_LENGTH, "array-length", READ_WRITE_FT));
		register("new-instance", ops(DalvikOperands.REGISTER, DalvikOperands.CLASS_TYPE),
				(inst, visitor) -> visitor.visitNewInstance(inst.operand(0, RegisterRef.class), inst.argument(1, ASTIdentifier.class)),
				metadata(EnumSet.of(TYPE_REFERENCE, REGISTER_WRITE, FALLTHROUGH),
						List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "new-instance",
						new DalvikLowering(NEW_INSTANCE)));
		register("new-array", ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER, DalvikOperands.CLASS_TYPE),
				(inst, visitor) -> visitor.visitNewArray(inst.operand(0, RegisterRef.class),
						inst.operand(1, RegisterRef.class), inst.argument(2, ASTIdentifier.class)),
				metadata(EnumSet.of(TYPE_REFERENCE, REGISTER_READ, REGISTER_WRITE, FALLTHROUGH),
						List.of(new OperandRole(2, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "new-array",
						new DalvikLowering(NEW_ARRAY)));
		register("filled-new-array", ops(DalvikOperands.REGISTER_ARRAY, DalvikOperands.FILLED_NEW_ARRAY_TYPE),
				(inst, visitor) -> visitor.visitFilledNewArray(inst.operand(0, RegisterOperands.class), inst.argument(1, ASTIdentifier.class)),
				metadata(EnumSet.of(TYPE_REFERENCE, REGISTER_READ, REGISTER_WRITE, FALLTHROUGH),
						List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "filled-new-array",
						new DalvikLowering(FILLED_NEW_ARRAY)));
		register("filled-new-array/range", ops(DalvikOperands.REGISTER_RANGE, DalvikOperands.FILLED_NEW_ARRAY_TYPE),
				(inst, visitor) -> visitor.visitFilledNewArray(inst.operand(0, RegisterOperands.class), inst.argument(1, ASTIdentifier.class)),
				metadata(EnumSet.of(TYPE_REFERENCE, REGISTER_READ, REGISTER_WRITE, FALLTHROUGH),
						List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "filled-new-array/range",
						new DalvikLowering(FILLED_NEW_ARRAY_RANGE)));
		register("fill-array-data", ops(DalvikOperands.REGISTER, DalvikOperands.DATA_ARRAY),
				(inst, visitor) -> visitor.visitFillArrayData(inst.operand(0, RegisterRef.class), inst.operand(1, ArrayData.class)),
				lowering(FILL_ARRAY_DATA, "fill-array-data", READ_FT, PAYLOAD));
		register("throw", ops(DalvikOperands.REGISTER),
				(inst, visitor) -> visitor.visitThrow(inst.operand(0, RegisterRef.class)),
				lowering(THROW, "throw", THROW_TRAIT));
		register("goto", ops(DefaultOperands.LABEL),
				(inst, visitor) -> visitor.visitGoto(inst.argument(0)),
				metadata(EnumSet.of(UNCONDITIONAL_BRANCH),
						List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, "goto", new DalvikLowering(GOTO)));
		register("packed-switch", ops(DalvikOperands.REGISTER, DalvikOperands.PACKED_SWITCH),
				(inst, visitor) -> visitor.visitPackedSwitch(inst.operand(0, RegisterRef.class), inst.operand(1, PackedSwitchPayload.class)),
				metadata(EnumSet.of(SWITCH, PAYLOAD, REGISTER_READ),
						List.of(new OperandRole(1, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.PACKED, "packed-switch",
						new DalvikLowering(PACKED_SWITCH)));
		register("sparse-switch", ops(DalvikOperands.REGISTER, DalvikOperands.SPARSE_SWITCH),
				(inst, visitor) -> visitor.visitSparseSwitch(inst.operand(0, RegisterRef.class), inst.operand(1, SparseSwitchPayload.class)),
				metadata(EnumSet.of(SWITCH, PAYLOAD, REGISTER_READ),
						List.of(new OperandRole(1, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.SPARSE, "sparse-switch",
						new DalvikLowering(SPARSE_SWITCH)));

		registerCmp();
		registerBinaryOperation();
		registerBinary2AddrOperation();
		registerBinaryLiteralOperation();
		registerIf();
		registerIfZero();
		registerArrayOperation();
		registerVirtualFieldOperation();
		registerStaticFieldOperation();
		registerInvoke();
		registerInvokeCustom();
		registerInvokePolymorphic();
		registerUnaryOperation();

		register("line", ops(DefaultOperands.INTEGER),
				(inst, visitor) -> visitor.visitLineNumber(inst.argument(0, ASTNumber.class)),
				sourceMetadata(EnumSet.of(PSEUDO, DEBUG_METADATA), List.of(), null, "line"));
	}

	void registerBinaryOperation() {
		BINARY_OPCODES.forEach((name, opcode) -> {
			int[] wide = binaryOperationWidths(name);
			register(name,
					ops(registerOperands(3, wide)),
					(inst, visitor) -> visitor.visitBinaryOperation(
							inst.lowering(DalvikLowering.class),
							inst.operand(0, RegisterRef.class),
							inst.operand(1, RegisterRef.class),
							inst.operand(2, RegisterRef.class)),
					lowering(opcode, name, READ_WRITE_FT, wideRegisters(wide)));
		});
	}

	void registerBinary2AddrOperation() {
		BINARY_2ADDR_OPCODES.forEach((name, opcode) -> {
			int[] wide = binary2AddrOperationWidths(name);
			register(name,
					ops(registerOperands(2, wide)),
					(inst, visitor) -> visitor.visitBinary2AddrOperation(
							inst.lowering(DalvikLowering.class),
							inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class)),
					lowering(opcode, name, READ_WRITE_FT, wideRegisters(wide)));
		});
	}

	void registerBinaryLiteralOperation() {
		BINARY_LITERAL_16_OPCODES.forEach((name, opcode) -> register(name,
				ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER, DalvikOperands.LITERAL_16),
				(inst, visitor) -> visitor.visitBinaryLiteralOperation(
						inst.lowering(DalvikLowering.class),
						inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class),
						inst.operand(2, SignedLiteral.class)),
				lowering(opcode, name, READ_WRITE_FT)));
		BINARY_LITERAL_8_OPCODES.forEach((name, opcode) -> register(name,
				ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER, DalvikOperands.LITERAL_8),
				(inst, visitor) -> visitor.visitBinaryLiteralOperation(
						inst.lowering(DalvikLowering.class),
						inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class),
						inst.operand(2, SignedLiteral.class)),
				lowering(opcode, name, READ_WRITE_FT)));
	}

	void registerCmp() {
		COMPARE_OPCODES.forEach((name, opcode) -> {
			// The compared values are wide on the long and double forms, but the destination is the int
			// result, so it is never wide; the float forms are narrow throughout.
			int[] wide = isWideRegisterForm(name) ? new int[]{1, 2} : new int[0];
			register(name,
					ops(registerOperands(3, wide)),
					(inst, visitor) -> visitor.visitCmp(
							inst.lowering(DalvikLowering.class),
							inst.operand(0, RegisterRef.class),
							inst.operand(1, RegisterRef.class),
							inst.operand(2, RegisterRef.class)),
					lowering(opcode, name, READ_WRITE_FT, wideRegisters(wide)));
		});
	}

	void registerIf() {
		IF_OPCODES.forEach((name, opcode) -> register(name,
				ops(DalvikOperands.REGISTER, DalvikOperands.REGISTER, DefaultOperands.LABEL),
				(inst, visitor) -> visitor.visitIf(
						inst.lowering(DalvikLowering.class),
						inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class), inst.argument(2)),
				metadata(EnumSet.of(CONDITIONAL_BRANCH, REGISTER_READ, FALLTHROUGH),
						List.of(new OperandRole(2, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, name, new DalvikLowering(opcode))));
	}

	void registerIfZero() {
		IF_ZERO_OPCODES.forEach((name, opcode) -> register(name,
				ops(DalvikOperands.REGISTER, DefaultOperands.LABEL),
				(inst, visitor) -> visitor.visitIfZero(
						inst.lowering(DalvikLowering.class),
						inst.operand(0, RegisterRef.class), inst.argument(1)),
				metadata(EnumSet.of(CONDITIONAL_BRANCH, REGISTER_READ, FALLTHROUGH),
						List.of(new OperandRole(1, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, name, new DalvikLowering(opcode))));
	}

	void registerArrayOperation() {
		ARRAY_OPCODES.forEach((name, opcode) -> {
			int[] wide = WIDE_ARRAY_OPERANDS.contains(name) ? new int[]{0} : new int[0];
			register(name,
					ops(registerOperands(3, wide)),
					(inst, visitor) -> visitor.visitArrayOperation(
							inst.lowering(DalvikLowering.class),
							inst.operand(0, RegisterRef.class),
							inst.operand(1, RegisterRef.class),
							inst.operand(2, RegisterRef.class)),
					lowering(opcode, name, READ_WRITE_FT, wideRegisters(wide)));
		});
	}

	void registerVirtualFieldOperation() {
		INSTANCE_FIELD_OPCODES.forEach((name, opcode) -> {
			int[] wide = WIDE_FIELD_OPERANDS.contains(name) ? new int[]{0} : new int[0];
			register(name,
					ops(registerOperandsThen(registerOperands(2, wide), DefaultOperands.MEMBER_PATH,
							DefaultOperands.FIELD_DESCRIPTOR)),
					(inst, visitor) -> visitor.visitVirtualFieldOperation(
							inst.lowering(DalvikLowering.class),
							inst.operand(0, RegisterRef.class),
							inst.operand(1, RegisterRef.class),
							inst.operand(2, MemberPath.class),
							inst.argument(3, ASTIdentifier.class)),
					lowering(opcode, name, READ_FT, fieldRoles(wide, 2), FIELD_REFERENCE));
		});
	}

	void registerStaticFieldOperation() {
		STATIC_FIELD_OPCODES.forEach((name, opcode) -> {
			int[] wide = WIDE_FIELD_OPERANDS.contains(name) ? new int[]{0} : new int[0];
			register(name,
					ops(registerOperandsThen(registerOperands(1, wide), DefaultOperands.MEMBER_PATH, DefaultOperands.FIELD_DESCRIPTOR)),
					(inst, visitor) -> visitor.visitStaticFieldOperation(
							inst.lowering(DalvikLowering.class),
							inst.operand(0, RegisterRef.class),
							inst.operand(1, MemberPath.class),
							inst.argument(2, ASTIdentifier.class)),
					lowering(opcode, name, READ_FT, fieldRoles(wide, 1), FIELD_REFERENCE));
		});
	}

	void registerInvoke() {
		INVOKE_OPCODES.forEach((name, opcode) -> register(name,
				ops(registerOperand(name), DefaultOperands.MEMBER_PATH, DefaultOperands.METHOD_DESCRIPTOR),
				(inst, visitor) -> visitor.visitInvoke(
						inst.lowering(DalvikLowering.class),
						inst.operand(0, RegisterOperands.class),
						inst.operand(1, MemberPath.class),
						inst.argument(2, ASTIdentifier.class)),
				lowering(opcode, name, INVOKE_TRAIT, List.of(new OperandRole(1, OperandRole.RoleKind.MEMBER, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.MEMBER)))));
	}

	void registerInvokeCustom() {
		register("invoke-custom",
				ops(DalvikOperands.REGISTER_ARRAY, DefaultOperands.LITERAL, DefaultOperands.DESCRIPTOR, DalvikOperands.HANDLE, DalvikOperands.ARGS_ARRAY),
				(inst, visitor) -> visitor.visitInvokeCustom(
						inst.operand(0, RegisterOperands.class),
						inst.argument(1, ASTIdentifier.class),
						inst.argument(2, ASTIdentifier.class),
						inst.argument(3, ASTElement.class),
						inst.argumentArray(4)),
				lowering(INVOKE_CUSTOM, "invoke-custom", INVOKE_CUSTOM_TRAIT));
		register("invoke-custom/range",
				ops(DalvikOperands.REGISTER_RANGE, DefaultOperands.LITERAL, DefaultOperands.DESCRIPTOR, DalvikOperands.HANDLE, DalvikOperands.ARGS_ARRAY),
				(inst, visitor) -> visitor.visitInvokeCustom(
						inst.operand(0, RegisterOperands.class),
						inst.argument(1, ASTIdentifier.class),
						inst.argument(2, ASTIdentifier.class),
						inst.argument(3, ASTElement.class),
						inst.argumentArray(4)),
				lowering(INVOKE_CUSTOM_RANGE, "invoke-custom/range", INVOKE_CUSTOM_TRAIT));
	}

	void registerInvokePolymorphic() {
		register("invoke-polymorphic",
				ops(DalvikOperands.REGISTER_ARRAY, DefaultOperands.MEMBER_PATH, DefaultOperands.METHOD_DESCRIPTOR, DefaultOperands.METHOD_DESCRIPTOR),
				(inst, visitor) -> visitor.visitInvokePolymorphic(
						inst.operand(0, RegisterOperands.class),
						inst.operand(1, MemberPath.class),
						inst.argument(2, ASTIdentifier.class),
						inst.argument(3, ASTIdentifier.class)),
				metadata(EnumSet.copyOf(INVOKE_TRAIT),
						List.of(new OperandRole(1, OperandRole.RoleKind.MEMBER, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.MEMBER)),
						null, "invoke-polymorphic", new DalvikLowering(INVOKE_POLYMORPHIC)));
		register("invoke-polymorphic/range",
				ops(DalvikOperands.REGISTER_RANGE, DefaultOperands.MEMBER_PATH, DefaultOperands.METHOD_DESCRIPTOR, DefaultOperands.METHOD_DESCRIPTOR),
				(inst, visitor) -> visitor.visitInvokePolymorphic(
						inst.operand(0, RegisterOperands.class),
						inst.operand(1, MemberPath.class),
						inst.argument(2, ASTIdentifier.class),
						inst.argument(3, ASTIdentifier.class)),
				metadata(EnumSet.copyOf(INVOKE_TRAIT),
						List.of(new OperandRole(1, OperandRole.RoleKind.MEMBER, OperandRole.WidthPolicy.SINGLE,
								OperandRole.ReferencePolicy.MEMBER)), null, "invoke-polymorphic/range",
						new DalvikLowering(INVOKE_POLYMORPHIC_RANGE)));
	}

	void registerUnaryOperation() {
		UNARY_OPCODES.forEach((name, opcode) -> {
			int[] wide = UNARY_WIDE_OPERANDS.getOrDefault(name, new int[0]);
			register(name,
					ops(registerOperands(2, wide)),
					(inst, visitor) -> visitor.visitUnaryOperation(
							inst.lowering(DalvikLowering.class),
							inst.operand(0, RegisterRef.class), inst.operand(1, RegisterRef.class)),
					lowering(opcode, name, READ_WRITE_FT, wideRegisters(wide)));
		});
	}

	/**
	 * @param opcode
	 * 		Dex opcode this instruction encodes as.
	 * @param name
	 * 		Registered source mnemonic, which is also its canonical name.
	 * @param traits
	 * 		Traits shared by this instruction's family.
	 * @param extra
	 * 		Traits this instruction adds beyond its family.
	 *
	 * @return Metadata carrying the declared dex lowering and traits, with no operand roles yet.
	 */
	private InstructionMetadata lowering(int opcode, String name, Set<InstructionTrait> traits,
	                                     InstructionTrait... extra) {
		return lowering(opcode, name, traits, List.of(), extra);
	}

	/**
	 * @param opcode
	 * 		Dex opcode this instruction encodes as.
	 * @param name
	 * 		Registered source mnemonic, which is also its canonical name.
	 * @param traits
	 * 		Traits shared by this instruction's family.
	 * @param roles
	 * 		Operand roles declared for this instruction.
	 * @param extra
	 * 		Traits this instruction adds beyond its family.
	 *
	 * @return Metadata carrying the declared dex lowering, traits and roles.
	 */
	private InstructionMetadata lowering(int opcode, String name, Set<InstructionTrait> traits,
	                                     List<OperandRole> roles, InstructionTrait... extra) {
		EnumSet<InstructionTrait> declared = EnumSet.copyOf(traits);
		declared.addAll(List.of(extra));
		return metadata(declared, roles, null, name, new DalvikLowering(opcode));
	}

	/**
	 * @param registers
	 * 		Register operands, wide where declared.
	 * @param trailing
	 * 		Operands that follow the registers in declaration order.
	 *
	 * @return Operand schema with {@code registers} followed by {@code trailing} operands.
	 */
	private static Operands[] registerOperandsThen(DalvikOperands[] registers, Operands... trailing) {
		Operands[] all = new Operands[registers.length + trailing.length];
		System.arraycopy(registers, 0, all, 0, registers.length);
		System.arraycopy(trailing, 0, all, registers.length, trailing.length);
		return all;
	}

	/**
	 * @param wide
	 * 		Positions holding a wide value.
	 * @param memberIndex
	 * 		Position holding the member reference.
	 *
	 * @return Roles for the wide register operands and the member reference.
	 */
	private static List<OperandRole> fieldRoles(int[] wide, int memberIndex) {
		List<OperandRole> roles = new ArrayList<>(wideRegisters(wide));
		roles.add(new OperandRole(memberIndex, OperandRole.RoleKind.MEMBER, OperandRole.WidthPolicy.SINGLE,
				OperandRole.ReferencePolicy.MEMBER));
		return List.copyOf(roles);
	}

	/**
	 * @param indices
	 * 		Positions holding a wide value.
	 *
	 * @return Roles declaring each of those operands wide, so the allocator does not have to infer the
	 * extra word from the mnemonic.
	 */
	private static List<OperandRole> wideRegisters(int... indices) {
		List<OperandRole> roles = new ArrayList<>(indices.length);
		for (int index : indices)
			roles.add(new OperandRole(index, OperandRole.RoleKind.VARIABLE, OperandRole.WidthPolicy.WIDE,
					OperandRole.ReferencePolicy.NONE));
		return List.copyOf(roles);
	}

	/**
	 * @param count
	 * 		Number of register operands.
	 * @param wide
	 * 		Positions holding a wide value.
	 *
	 * @return Operand schema for each register position, wide where {@code wide} names the position.
	 */
	private static DalvikOperands[] registerOperands(int count, int... wide) {
		DalvikOperands[] operands = new DalvikOperands[count];
		for (int index = 0; index < count; index++)
			operands[index] = isWidePosition(wide, index) ? DalvikOperands.REGISTER_WIDE : DalvikOperands.REGISTER;
		return operands;
	}

	/**
	 * @param wide
	 * 		Positions holding a wide value.
	 * @param index
	 * 		Position to test.
	 *
	 * @return {@code true} when {@code index} is one of {@code wide}.
	 */
	private static boolean isWidePosition(int[] wide, int index) {
		for (int candidate : wide)
			if (candidate == index)
				return true;
		return false;
	}

	/**
	 * @param name
	 * 		Registered source mnemonic.
	 *
	 * @return Positions holding a wide value for a three-operand binary operation. A long or double
	 * operation is wide throughout except for a long shift, whose distance is a narrow int; the int
	 * and float operations are narrow throughout.
	 */
	private static int[] binaryOperationWidths(String name) {
		if (isLongShift(name))
			return new int[]{0, 1};
		return isWideRegisterForm(name) ? new int[]{0, 1, 2} : new int[0];
	}

	/**
	 * @param name
	 * 		Registered source mnemonic.
	 *
	 * @return Positions holding a wide value for a two-operand {@code /2addr} operation. The destination
	 * is always wide on a long or double form; the second operand is wide too unless the form is a
	 * long shift, whose distance is a narrow int.
	 */
	private static int[] binary2AddrOperationWidths(String name) {
		if (isLongShift(name))
			return new int[]{0};
		return isWideRegisterForm(name) ? new int[]{0, 1} : new int[0];
	}

	/**
	 * @param name
	 * 		Registered source mnemonic.
	 *
	 * @return {@code true} when {@code name} shifts a long by an int distance. The distance is a single
	 * narrow register even though the name ends in {@code -long}, which is why the operand width
	 * cannot be derived from the suffix alone.
	 */
	private static boolean isLongShift(String name) {
		String base = name.endsWith("/2addr") ? name.substring(0, name.length() - "/2addr".length()) : name;
		return base.equals("shl-long") || base.equals("shr-long") || base.equals("ushr-long");
	}

	/**
	 * @param name
	 * 		Registered source mnemonic.
	 *
	 * @return List-shaped register operand for plain mnemonics, range-shaped for {@code /range} forms.
	 */
	private static DalvikOperands registerOperand(String name) {
		return name.endsWith("/range") ? DalvikOperands.REGISTER_RANGE : DalvikOperands.REGISTER_ARRAY;
	}

	/**
	 * @param name
	 * 		Registered source mnemonic of a single register operand.
	 *
	 * @return Wide register operand for forms that consume two register words, otherwise a single-word register operand.
	 */
	private static DalvikOperands registerFor(String name) {
		return isWideRegisterForm(name) ? DalvikOperands.REGISTER_WIDE : DalvikOperands.REGISTER;
	}

	/**
	 * @param name
	 * 		Registered source mnemonic.
	 *
	 * @return {@code true} when every register operand of {@code name} holds a wide value.
	 */
	private static boolean isWideRegisterForm(String name) {
		// The long and double families are wide throughout, and the four explicit wide forms below
		// are the remaining single-register operand instructions whose value spans two words.
		return name.endsWith("-long")
				|| name.endsWith("-double")
				|| name.endsWith("-long/2addr")
				|| name.endsWith("-double/2addr")
				|| name.equals("move-wide")
				|| name.equals("move-result-wide")
				|| name.equals("return-wide")
				|| name.equals("const-wide");
	}
}
