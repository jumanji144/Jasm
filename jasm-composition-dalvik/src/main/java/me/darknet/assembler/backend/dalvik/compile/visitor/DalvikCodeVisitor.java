package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.backend.dalvik.compile.DalvikConstantMapper;
import me.darknet.assembler.backend.dalvik.instructions.ArithmeticLowering;
import static me.darknet.assembler.backend.dalvik.instructions.ArithmeticLowering.NONE;
import me.darknet.assembler.backend.dalvik.instructions.ArrayData;
import me.darknet.assembler.backend.dalvik.instructions.DalvikArrayLiterals;
import me.darknet.assembler.backend.dalvik.instructions.DalvikLowering;
import me.darknet.assembler.backend.dalvik.instructions.IntegerLiteral;
import me.darknet.assembler.backend.dalvik.instructions.PackedSwitchPayload;
import me.darknet.assembler.backend.dalvik.instructions.RegisterOperands;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRef;
import me.darknet.assembler.backend.dalvik.instructions.SparseSwitchPayload;
import me.darknet.assembler.backend.dalvik.visitor.ASTDalvikInstructionVisitor;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.error.Severity;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.OperandValue;
import me.darknet.assembler.instructions.SemanticInstruction;
import me.darknet.assembler.instructions.ValidatedOperand;
import me.darknet.assembler.util.Location;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.HandleConstant;
import me.darknet.dex.tree.definitions.instructions.ArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.ArrayLengthInstruction;
import me.darknet.dex.tree.definitions.instructions.Binary2AddrInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryLiteralInstruction;
import me.darknet.dex.tree.definitions.instructions.BranchInstruction;
import me.darknet.dex.tree.definitions.instructions.BranchZeroInstruction;
import me.darknet.dex.tree.definitions.instructions.CheckCastInstruction;
import me.darknet.dex.tree.definitions.instructions.CompareInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodHandleInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstStringInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import me.darknet.dex.tree.definitions.instructions.FillArrayDataInstruction;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.GotoInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceOfInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.MonitorInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveExceptionInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveObjectInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveResultInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveWideInstruction;
import me.darknet.dex.tree.definitions.instructions.NewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.NewInstanceInstruction;
import me.darknet.dex.tree.definitions.instructions.NopInstruction;
import me.darknet.dex.tree.definitions.instructions.PackedSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.Result;
import me.darknet.dex.tree.definitions.instructions.Return;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import me.darknet.dex.tree.definitions.instructions.SparseSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.StaticFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.ThrowInstruction;
import me.darknet.dex.tree.definitions.instructions.UnaryInstruction;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.ReferenceType;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visits Dalvik instruction syntax and emits dex code through a {@link CodeBuilder}.
 */
public class DalvikCodeVisitor implements ASTDalvikInstructionVisitor, Opcodes {
	/** Width of the register fields of the 12x, 22s, and 35c encodings, which name registers {@code v0} to {@code v15}. */
	private static final int FOUR_BITS = 4;
	/** Width of the register fields of the 21c, 22b, and 23x encodings, and of the count of a range. */
	private static final int EIGHT_BITS = 8;
	/** Width of the first-register field of a range encoding, and of the signed literal of the 22s literal form. */
	private static final int SIXTEEN_BITS = 16;

	private final CodeBuilder codeBuilder;
	private final DiagnosticSink sink;
	private SemanticInstruction currentInstructionAst;
	private final Map<Instruction, ASTInstruction> instructionToSource = new IdentityHashMap<>();
	private final List<Instruction> emittedInstructions = new ArrayList<>();
	private final List<ASTInstruction> emittedSources = new ArrayList<>();
	private final Map<String, Integer> registerMap = new HashMap<>();
	private final Set<Integer> usedRegisters = new HashSet<>();
	private final Map<String, Label> labels = new HashMap<>();
	private final Map<Label, Integer> labelOrder = new IdentityHashMap<>();
	private final List<TryCatch> tryCatches = new ArrayList<>();
	private int nextLabelOrder;
	private final int registerLimit;
	private final int parameterBase;
	private final @Nullable Location registerDeclaration;
	private int nextLocalRegister;
	private int registerCount;
	private int outRegisters;
	private boolean reportedRegisterLimit;
	private int pendingLineNumber = Label.UNASSIGNED;

	/**
	 * @param codeBuilder
	 * 		Builder collecting the emitted dex instructions.
	 * @param sink
	 * 		Sink reporting instructions this backend cannot encode.
	 * @param initialRegisterMap
	 * 		Registers already bound to names, which are the method's parameters.
	 * @param registerLimit
	 * 		Register count declared by the method, or {@code -1} when none was declared.
	 * @param parameterBase
	 * 		First register slot holding a parameter, or {@code -1} when no register count was declared.
	 * @param registerDeclaration
	 * 		Location of the {@code registers} declaration, or {@code null} when the method declared no count.
	 * @param explicitRegisters
	 * 		Physical slots addressed by explicit {@code vN} operands in the whole method.
	 */
	public DalvikCodeVisitor(@NotNull CodeBuilder codeBuilder, @NotNull DiagnosticSink sink,
	                         @NotNull Map<String, Integer> initialRegisterMap,
	                         int registerLimit, int parameterBase,
	                         @Nullable Location registerDeclaration,
	                         @NotNull Set<Integer> explicitRegisters) {
		this.codeBuilder = codeBuilder;
		this.sink = sink;
		this.registerLimit = registerLimit;
		this.parameterBase = parameterBase;
		this.registerDeclaration = registerDeclaration;
		usedRegisters.addAll(explicitRegisters);
		initialRegisterMap.forEach((name, index) -> {
			registerMap.put(name, index);
			usedRegisters.add(index);
			registerCount = Math.max(registerCount, index + 1);
		});
		if (registerLimit >= 0)
			registerCount = Math.max(registerCount, registerLimit);
	}

	/**
	 * @param ref
	 * 		Resolved register operand.
	 *
	 * @return Slot index of the register, allocating a slot for a named register the first time it is used.
	 */
	private int indexOf(@NotNull RegisterRef ref) {
		Integer index = ref.index();
		if (index == null) {
			index = getRegisterIndex(ref.name());
		} else {
			ensureRegisterAllowed(index);
			usedRegisters.add(index);
			registerCount = Math.max(registerCount, index + 1);
		}

		// A wide value occupies the slot after its own, so the last declared slot cannot hold one.
		if (ref.width() == OperandRole.WidthPolicy.WIDE) {
			ensureRegisterAllowed(index + 1);
			usedRegisters.add(index + 1);
			registerCount = Math.max(registerCount, index + 2);
		}

		return index;
	}

	/**
	 * @param registerName
	 * 		Name to allocate a slot for.
	 *
	 * @return Slot index bound to the name. A name already bound keeps its slot; a new name takes the first
	 * free slot below the parameter base that is not occupied by a parameter or explicit {@code vN} operand.
	 * When no such slot exists the problem is reported and
	 * {@link #localCeiling()} is returned, which is outside the declared count so the compile still fails.
	 */
	public int getRegisterIndex(String registerName) {
		Integer existing = registerMap.get(registerName);
		if (existing != null)
			return existing;

		// The first free slot below the parameter base is the one to allocate.
		// The allocator does not try to find a free slot above the base,
		// because that would require a larger register count, which the method's declaration does not allow.
		int ceiling = localCeiling();
		int index = nextLocalRegister;
		while (index < ceiling && usedRegisters.contains(index)) {
			index++;
		}
		if (index >= ceiling) {
			reportNoFreeRegister(registerName, index, ceiling);
			return index;
		}

		registerMap.put(registerName, index);
		usedRegisters.add(index);
		nextLocalRegister = index + 1;
		registerCount = Math.max(registerCount, index + 1);
		return index;
	}

	/**
	 * @return Highest slot index a local may use, exclusive: everything below the parameter base. Unbounded
	 * when the method declared no register count, because slots are then allocated to fit.
	 */
	private int localCeiling() {
		if (parameterBase >= 0)
			return parameterBase;

		// One below the maximum keeps 'index + 1' from wrapping when the count is unbounded.
		return registerLimit >= 0 ? registerLimit : Integer.MAX_VALUE - 1;
	}

	/**
	 * Reports that a name cannot be given a slot because the declared count leaves no room for a local.
	 *
	 * @param registerName
	 * 		Name that needed a slot.
	 * @param index
	 * 		Slot the allocator stopped at, which is the first position past the last usable one.
	 * @param ceiling
	 * 		Highest usable slot index, exclusive.
	 */
	private void reportNoFreeRegister(String registerName, int index, int ceiling) {
		String detail = ceiling == 0
				? "every declared register holds a parameter"
				: "all " + ceiling + " slots below the parameter base are in use";
		sink.add(new Diagnostic(Severity.ERROR, DiagnosticPhase.BACKEND_EMISSION,
				DiagnosticCode.REGISTER_LIMIT,
				"No register available for '" + registerName + "': " + detail
						+ ", so a larger register count is required",
				currentInstructionAst == null ? null : currentInstructionAst.location(),
				registerDeclaration == null ? List.of() : List.of(registerDeclaration)));
		reportedRegisterLimit = true;
		// Keep the allocator from handing the same unusable slot to the next name.
		nextLocalRegister = Math.max(nextLocalRegister, index);
	}

	/**
	 * Reports a register reference outside the declared register count.
	 * <p>
	 * The diagnostic is recorded and traversal continues so the instruction's other operands are validated.
	 * Callers must therefore tolerate an out-of-range slot after this method reports an error.
	 *
	 * @param index
	 * 		Slot index the instruction wants to use.
	 */
	private void ensureRegisterAllowed(int index) {
		if (index >= 0 && (registerLimit < 0 || index < registerLimit))
			return;

		// The declared count is what makes the slot illegal, so it is reported alongside the instruction.
		sink.add(new Diagnostic(Severity.ERROR, DiagnosticPhase.BACKEND_EMISSION,
				DiagnosticCode.REGISTER_LIMIT, "Register exceeds declared register count: v" + index,
				currentInstructionAst == null ? null : currentInstructionAst.location(),
				registerDeclaration == null ? List.of() : List.of(registerDeclaration)));
		reportedRegisterLimit = true;
	}

	/**
	 * @return {@code true} when {@link #ensureRegisterAllowed(int)} already reported an overrun for this method.
	 */
	public boolean hasReportedRegisterLimit() {
		return reportedRegisterLimit;
	}

	/**
	 * @return Highest slot index used by the method.
	 */
	public int getRegisterCount() {
		return registerCount;
	}

	/**
	 * @return Highest slot index used by the method for outgoing arguments.
	 */
	public int getOutRegisters() {
		return outRegisters;
	}

	/**
	 * Updates the outgoing register count to the maximum of its current value and the given count.
	 *
	 * @param count
	 * 		Count of registers used for outgoing arguments by an instruction.
	 */
	private void updateOutRegisters(int count) {
		outRegisters = Math.max(outRegisters, count);
	}

	/**
	 * @return Try-catch blocks emitted by this visitor.
	 */
	public @NotNull List<@NotNull TryCatch> getTryCatches() {
		return List.copyOf(tryCatches);
	}

	/**
	 * @param codeInstructions
	 * 		List of instructions emitted by the code builder.
	 *
	 * @return Map of executable instructions to their source AST instructions.
	 */
	public @NotNull Map<Instruction, ASTInstruction> buildSourceMap(@NotNull List<Instruction> codeInstructions) {
		List<Instruction> executableInstructions = new ArrayList<>();
		for (Instruction instruction : codeInstructions)
			if (!(instruction instanceof Label))
				executableInstructions.add(instruction);

		if (executableInstructions.size() != emittedInstructions.size())
			throw new IllegalStateException("Code builder changed the number of emitted executable instructions");

		boolean preservedIdentities = true;
		for (int index = 0; index < emittedInstructions.size(); index++) {
			if (emittedInstructions.get(index) != executableInstructions.get(index)) {
				preservedIdentities = false;
				break;
			}
		}

		Map<Instruction, ASTInstruction> sources = new IdentityHashMap<>();
		if (preservedIdentities) {
			sources.putAll(instructionToSource);
		} else {
			for (int index = 0; index < executableInstructions.size(); index++)
				sources.put(executableInstructions.get(index), emittedSources.get(index));
		}
		return sources;
	}

	/**
	 * Adds an instruction to the builder, inserting a label first if a line number was pending.
	 *
	 * @param instruction
	 * 		Instruction to add.
	 */
	private void addInstruction(Instruction instruction) {
		if (pendingLineNumber != Label.UNASSIGNED) {
			Label label = new Label();
			label.lineNumber(pendingLineNumber);
			codeBuilder.add(label);
			pendingLineNumber = Label.UNASSIGNED;
		}
		if (currentInstructionAst == null)
			throw new IllegalStateException("Executable instruction emitted without a source AST instruction");
		codeBuilder.add(instruction);
		ASTInstruction source = currentInstructionAst.source();
		instructionToSource.put(instruction, source);
		emittedInstructions.add(instruction);
		emittedSources.add(source);
	}

	/**
	 * @param name
	 * 		Source label name.
	 *
	 * @return Label bound to the name, creating a new one if the name was not used before.
	 */
	private @NotNull Label getLabel(@NotNull String name) {
		return labels.computeIfAbsent(name, ignored -> new Label());
	}

	/**
	 * @param type
	 * 		Source class descriptor.
	 *
	 * @return Class type the descriptor encodes.
	 */
	private static @NotNull ClassType parseClassType(@NotNull ASTIdentifier type) {
		return new TypeParser(type.literal()).requireClassType();
	}

	/**
	 * @param owner
	 * 		Source class descriptor or array descriptor.
	 *
	 * @return Reference type the descriptor encodes.
	 */
	private static @NotNull ReferenceType parseReferenceType(@NotNull String owner) {
		return owner.startsWith("[")
				? Types.referenceTypeFromDescriptor(owner)
				: Types.instanceTypeFromInternalName(owner);
	}

	/**
	 * @param descriptor
	 * 		Source method descriptor.
	 *
	 * @return Method type the descriptor encodes.
	 */
	private static @NotNull MethodType parseMethodType(@NotNull ASTIdentifier descriptor) {
		return Types.methodTypeFromDescriptor(descriptor.literal());
	}

	/**
	 * @param type
	 * 		Source class descriptor.
	 *
	 * @return Instance type the descriptor encodes.
	 */
	private static @NotNull InstanceType parseInstanceType(@NotNull ASTIdentifier type) {
		String literal = type.literal();
		if (literal.startsWith("L")) {
			ClassType classType = new TypeParser(literal).requireClassType();
			if (classType instanceof InstanceType instanceType) {
				return instanceType;
			}
			throw new IllegalStateException("Expected instance type, got: " + literal);
		}
		return Types.instanceTypeFromInternalName(literal);
	}

	/**
	 * @param registers
	 * 		Resolved register operands.
	 * @param range
	 * 		Whether the instruction uses a range of registers.
	 *
	 * @return Count of words used by the instruction's register operands.
	 */
	private static int argumentWordCount(@NotNull List<Integer> registers, boolean range) {
		if (range)
			return registers.get(1) - registers.get(0) + 1;

		return registers.size();
	}

	/**
	 * @param registers
	 * 		Resolved register operand.
	 *
	 * @return Slot indices in source order, with named registers allocated as a side effect.
	 */
	private List<Integer> indexOfAll(@NotNull RegisterOperands registers) {
		List<Integer> indices = new ArrayList<>(registers.registers().size());
		for (RegisterRef ref : registers.registers())
			indices.add(indexOf(ref));
		return indices;
	}

	/**
	 * @param literal
	 * 		Source spelling.
	 *
	 * @return Bits of the named value as a {@code float}.
	 */
	private static int parseSpecialNumber(@NotNull String literal) {
		String normalized = literal.toLowerCase();
		return switch (normalized) {
			case "nan", "nand", "nanf" -> Float.floatToIntBits(Float.NaN);
			case "infinity", "+infinity", "infinityd", "+infinityd", "infinityf", "+infinityf" ->
					Float.floatToIntBits(Float.POSITIVE_INFINITY);
			case "-infinity", "-infinityd", "-infinityf" -> Float.floatToIntBits(Float.NEGATIVE_INFINITY);
			default -> throw new IllegalStateException("Unsupported const literal: " + literal);
		};
	}

	/**
	 * Parses one of the special number spellings into the 64-bit form of the value it names.
	 *
	 * @param literal
	 * 		Source spelling.
	 *
	 * @return Bits of the named value as a {@code double}.
	 *
	 * @see #parseSpecialNumber(String)
	 */
	private static long parseSpecialWideNumber(@NotNull String literal) {
		String normalized = literal.toLowerCase();
		return switch (normalized) {
			case "nan", "nand", "nanf" -> Double.doubleToLongBits(Double.NaN);
			case "infinity", "+infinity", "infinityd", "+infinityd", "infinityf", "+infinityf" ->
					Double.doubleToLongBits(Double.POSITIVE_INFINITY);
			case "-infinity", "-infinityd", "-infinityf" -> Double.doubleToLongBits(Double.NEGATIVE_INFINITY);
			default -> throw new IllegalStateException("Unsupported const-wide literal: " + literal);
		};
	}

	/**
	 * @param values
	 * 		Integral or floating-point values to encode.
	 * @param elementWidth
	 * 		Width of each value in bytes, which must be 1, 2, 4, or 8.
	 *
	 * @return Little-endian encoding of the values as a byte array.
	 */
	private static byte[] encodeArrayData(@NotNull List<ASTNumber> values, int elementWidth) {
		ByteBuffer buffer = ByteBuffer.allocate(values.size() * elementWidth).order(ByteOrder.LITTLE_ENDIAN);
		for (ASTNumber number : values) {
			if (DalvikArrayLiterals.isFloating(number)) {
				// The resolver has already constrained the width to the literal's own size, so an
				// unsupported width here would mean the operand schema and this encoder disagree.
				switch (elementWidth) {
					case 4 ->
							buffer.putInt(Float.floatToRawIntBits(number.isWide() ? (float) number.asDouble() : number.asFloat()));
					case 8 -> buffer.putLong(Double.doubleToRawLongBits(number.asDouble()));
					default ->
							throw new IllegalStateException("Floating-point array data requires 4-byte or 8-byte elements");
				}
				continue;
			}

			long value = DalvikArrayLiterals.toIntegral(number);
			switch (elementWidth) {
				case 1 -> buffer.put((byte) value);
				case 2 -> buffer.putShort((short) value);
				case 4 -> buffer.putInt((int) value);
				case 8 -> buffer.putLong(value);
				default -> throw new IllegalStateException("Unsupported array element width: " + elementWidth);
			}
		}
		return buffer.array();
	}

	/**
	 * @param handle
	 * 		Source method handle constant.
	 *
	 * @return Method handle the constant encodes.
	 */
	@SuppressWarnings("DeconstructionCanBeUsed") // Don't want more qualified names if avoidable.
	private static @NotNull me.darknet.dex.tree.definitions.constant.Handle parseHandle(@NotNull ASTElement handle) {
		Constant constant = DalvikConstantMapper.fromConstant(handle);
		if (constant instanceof HandleConstant handleConstant)
			return handleConstant.handle();
		throw new IllegalStateException("Expected method handle constant");
	}

	/**
	 * @param arguments
	 * 		Source constant arguments.
	 *
	 * @return Constants the arguments encode.
	 */
	private static @NotNull List<Constant> parseConstants(@NotNull ASTArray arguments) {
		List<Constant> constants = new ArrayList<>(arguments.values().size());
		for (ASTElement argument : arguments.values())
			constants.add(DalvikConstantMapper.fromConstant(argument));
		return List.copyOf(constants);
	}

	@Override
	public void visitInstruction(@NotNull SemanticInstruction instruction) {
		currentInstructionAst = instruction;
	}

	@Override
	public void visitNop() {
		addInstruction(new NopInstruction());
	}

	@Override
	public void visitMove(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from) {
		int toIndex = indexOf(to);
		int fromIndex = indexOf(from);
		addInstruction(switch (lowering.opcode()) {
			case MOVE_OBJECT -> new MoveObjectInstruction(toIndex, fromIndex);
			case MOVE_WIDE -> new MoveWideInstruction(toIndex, fromIndex);
			default -> new MoveInstruction(toIndex, fromIndex);
		});
	}

	@Override
	public void visitMoveResult(@NotNull DalvikLowering lowering, RegisterRef to) {
		int toIndex = indexOf(to);
		addInstruction(switch (lowering.opcode()) {
			case MOVE_RESULT_OBJECT -> new MoveResultInstruction(Result.OBJECT, toIndex);
			case MOVE_RESULT_WIDE -> new MoveResultInstruction(Result.WIDE, toIndex);
			default -> new MoveResultInstruction(Result.NORMAL, toIndex);
		});
	}

	@Override
	public void visitMoveException(RegisterRef to) {
		addInstruction(new MoveExceptionInstruction(indexOf(to)));
	}

	@Override
	public void visitReturn(@NotNull DalvikLowering lowering, RegisterRef returnValue) {
		int returnIndex = indexOf(returnValue);
		addInstruction(switch (lowering.opcode()) {
			case RETURN_OBJECT -> new ReturnInstruction(returnIndex, Return.OBJECT);
			case RETURN_WIDE -> new ReturnInstruction(returnIndex, Return.WIDE);
			default -> new ReturnInstruction(returnIndex);
		});
	}

	@Override
	public void visitReturnVoid() {
		addInstruction(new ReturnInstruction());
	}

	@Override
	public void visitConst(@NotNull DalvikLowering lowering, RegisterRef to, ASTElement value) {
		// Every constant form names its destination in an eight-bit field, so a higher register has no encoding.
		int toIndex = indexOf(to);
		if (!checkRegisterField(to, toIndex, EIGHT_BITS))
			return;
		switch (lowering.opcode()) {
			case CONST, CONST_WIDE -> {
				boolean wide = lowering.opcode() == CONST_WIDE;
				if (value instanceof ASTNumber constValue) {
					if (wide) {
						if (constValue.isFloatingPoint()) {
							// Raw, not canonical: a NaN payload is part of the value being encoded, and the
							// canonicalizing forms collapse every NaN to one bit pattern. Writing through
							// them would silently change a class that depends on a specific payload.
							long longBits = constValue.isWide()
									? Double.doubleToRawLongBits(constValue.asDouble())
									: Double.doubleToRawLongBits(constValue.asFloat());
							addInstruction(new ConstWideInstruction(toIndex, longBits));
						} else {
							addInstruction(new ConstWideInstruction(toIndex, constValue.asLong()));
						}
					} else {
						if (constValue.isFloatingPoint()) {
							int intBits = constValue.isWide()
									? Float.floatToRawIntBits((float) constValue.asDouble())
									: Float.floatToRawIntBits(constValue.asFloat());
							addConst(toIndex, intBits);
						} else {
							addConst(toIndex, constValue.asInt());
						}
					}
				} else if (value instanceof ASTIdentifier identifier) {
					if (wide) {
						addInstruction(new ConstWideInstruction(toIndex, parseSpecialWideNumber(identifier.literal())));
					} else {
						addConst(toIndex, parseSpecialNumber(identifier.literal()));
					}
				}
			}
			case CONST_STRING -> addInstruction(new ConstStringInstruction(toIndex, ((ASTString) value).content()));
			case CONST_CLASS -> {
				if (!(value instanceof ASTIdentifier constValue)) {
					throw new IllegalStateException("const-class requires a class descriptor");
				}
				addInstruction(new ConstTypeInstruction(toIndex, parseClassType(constValue)));
			}
			case CONST_METHOD_HANDLE -> {
				Constant constant = DalvikConstantMapper.fromConstant(value);
				if (!(constant instanceof HandleConstant handle)) {
					throw new IllegalStateException("const-method-handle requires a method handle");
				}
				addInstruction(new ConstMethodHandleInstruction(toIndex, handle.handle()));
			}
			case CONST_METHOD_TYPE -> {
				if (!(value instanceof ASTIdentifier constValue)) {
					throw new IllegalStateException("const-method-type requires a method descriptor");
				}
				addInstruction(new ConstMethodTypeInstruction(
						toIndex,
						Types.methodTypeFromDescriptor(constValue.literal())
				));
			}
			default -> throw new IllegalStateException("Unsupported const opcode: " + lowering.opcode());
		}
	}

	@Override
	public void visitMonitorEnter(RegisterRef register) {
		addInstruction(new MonitorInstruction(indexOf(register), false));
	}

	@Override
	public void visitMonitorExit(RegisterRef register) {
		addInstruction(new MonitorInstruction(indexOf(register), true));
	}

	@Override
	public void visitCheckCast(RegisterRef register, ASTIdentifier type) {
		addInstruction(new CheckCastInstruction(indexOf(register), parseClassType(type)));
	}

	@Override
	public void visitInstanceOf(RegisterRef result, RegisterRef check, ASTIdentifier type) {
		addInstruction(new InstanceOfInstruction(
				indexOf(result),
				indexOf(check),
				parseClassType(type)
		));
	}

	@Override
	public void visitArrayLength(RegisterRef result, RegisterRef array) {
		addInstruction(new ArrayLengthInstruction(
				indexOf(result),
				indexOf(array)
		));
	}

	@Override
	public void visitNewInstance(RegisterRef result, ASTIdentifier type) {
		ClassType classType = parseClassType(type);
		if (!(classType instanceof InstanceType instanceType)) {
			throw new IllegalStateException("new-instance requires an instance type");
		}
		addInstruction(new NewInstanceInstruction(indexOf(result), instanceType));
	}

	@Override
	public void visitNewArray(RegisterRef result, RegisterRef size, ASTIdentifier type) {
		addInstruction(new NewArrayInstruction(
				indexOf(result),
				indexOf(size),
				parseClassType(type)
		));
	}

	@Override
	public void visitFilledNewArray(RegisterOperands args, ASTIdentifier type) {
		ClassType arrayType = parseClassType(type);
		List<Integer> registers = resolveRegisterOperands(args);
		if (registers == null)
			return;
		int registerCount = argumentWordCount(registers, args.isRange());
		updateOutRegisters(registerCount);
		if (args.isRange()) {
			addInstruction(new FilledNewArrayInstruction(arrayType, registerCount, registers.get(0)));
			return;
		}
		addInstruction(new FilledNewArrayInstruction(arrayType, toIntArray(registers)));
	}

	@Override
	public void visitFillArrayData(RegisterRef to, ArrayData data) {
		addInstruction(new FillArrayDataInstruction(
				indexOf(to),
				encodeArrayData(data.values(), data.elementWidth()),
				data.elementWidth()
		));
	}

	@Override
	public void visitThrow(RegisterRef exception) {
		addInstruction(new ThrowInstruction(indexOf(exception)));
	}

	@Override
	public void visitGoto(ASTIdentifier label) {
		addInstruction(new GotoInstruction(getLabel(label.literal())));
	}

	@Override
	public void visitPackedSwitch(RegisterRef register, PackedSwitchPayload payload) {
		List<Label> targetLabels = new ArrayList<>(payload.targets().size());
		for (String target : payload.targets()) {
			targetLabels.add(getLabel(target));
		}

		addInstruction(new PackedSwitchInstruction(
				indexOf(register),
				payload.first(),
				List.copyOf(targetLabels)
		));
	}

	@Override
	public void visitSparseSwitch(RegisterRef register, SparseSwitchPayload payload) {
		Map<Integer, Label> targets = new LinkedHashMap<>();
		for (Map.Entry<Integer, String> entry : payload.targets().entrySet()) {
			targets.put(entry.getKey(), getLabel(entry.getValue()));
		}

		addInstruction(new SparseSwitchInstruction(
				indexOf(register),
				Map.copyOf(targets)
		));
	}

	@Override
	public void visitCmp(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from1, RegisterRef from2) {
		addInstruction(new CompareInstruction(
				lowering.opcode(),
				indexOf(to),
				indexOf(from1),
				indexOf(from2)
		));
	}

	@Override
	public void visitBinaryOperation(@NotNull ArithmeticLowering lowering, RegisterRef to, RegisterRef from1,
	                                 RegisterRef from2) {
		// Every operand gets its slot before an encoding is chosen, so aliasing is decided by the resolved slots.
		int toIndex = indexOf(to);
		int fromIndex = indexOf(from1);
		int otherIndex = indexOf(from2);

		// The two-address form writes its first source, so it can stand in only when the destination is that slot.
		// Slots are compared rather than names, so a parameter and a local that share a slot still qualify.
		if (toIndex == fromIndex && lowering.twoAddress() != NONE
				&& fitsField(toIndex, FOUR_BITS) && fitsField(otherIndex, FOUR_BITS)) {
			addInstruction(new Binary2AddrInstruction(lowering.twoAddress(), toIndex, otherIndex));
			return;
		}

		// The ordinary form keeps the operands in their written order, and each of its three fields holds eight bits.
		boolean fits = checkRegisterField(to, toIndex, EIGHT_BITS);
		fits &= checkRegisterField(from1, fromIndex, EIGHT_BITS);
		fits &= checkRegisterField(from2, otherIndex, EIGHT_BITS);
		if (fits)
			addInstruction(new BinaryInstruction(lowering.register(), toIndex, fromIndex, otherIndex));
	}

	@Override
	public void visitBinaryLiteralOperation(@NotNull ArithmeticLowering lowering, RegisterRef to, RegisterRef from,
	                                        IntegerLiteral literal) {
		int toIndex = indexOf(to);
		int fromIndex = indexOf(from);
		int value = literal.value();

		// The eight-bit form is preferred, since its registers reach further. The sixteen-bit form is the fallback.
		if (lowering.literal8() != NONE && fitsSigned(value, EIGHT_BITS)
				&& fitsField(toIndex, EIGHT_BITS) && fitsField(fromIndex, EIGHT_BITS)) {
			addInstruction(new BinaryLiteralInstruction(lowering.literal8(), toIndex, fromIndex, value));
		} else if (lowering.literal16() != NONE && fitsSigned(value, SIXTEEN_BITS)
				&& fitsField(toIndex, FOUR_BITS) && fitsField(fromIndex, FOUR_BITS)) {
			addInstruction(new BinaryLiteralInstruction(lowering.literal16(), toIndex, fromIndex, value));
		} else {
			reportNoLiteralEncoding(lowering, literal, toIndex, fromIndex);
		}
	}

	@Override
	public void visitIf(@NotNull DalvikLowering lowering, RegisterRef a, RegisterRef b, ASTIdentifier label) {
		addInstruction(new BranchInstruction(
				lowering.opcode() - IF_EQ,
				indexOf(a),
				indexOf(b),
				getLabel(label.literal())
		));
	}

	@Override
	public void visitIfZero(@NotNull DalvikLowering lowering, RegisterRef a, ASTIdentifier label) {
		addInstruction(new BranchZeroInstruction(
				lowering.opcode() - IF_EQZ,
				indexOf(a),
				getLabel(label.literal())
		));
	}

	@Override
	public void visitArrayOperation(@NotNull DalvikLowering lowering, RegisterRef array, RegisterRef index, RegisterRef value) {
		addInstruction(new ArrayInstruction(
				lowering.opcode() - AGET,
				indexOf(array),
				indexOf(index),
				indexOf(value)
		));
	}

	@Override
	public void visitVirtualFieldOperation(@NotNull DalvikLowering lowering, RegisterRef value, RegisterRef instance,
	                                       MemberPath path, ASTIdentifier descriptor) {
		addInstruction(new InstanceFieldInstruction(
				lowering.opcode() - IGET,
				indexOf(value),
				indexOf(instance),
				Types.instanceTypeFromInternalName(path.owner()),
				path.name(),
				new TypeParser(descriptor.literal()).requireClassType()
		));
	}

	@Override
	public void visitStaticFieldOperation(@NotNull DalvikLowering lowering, RegisterRef value, MemberPath path,
	                                      ASTIdentifier descriptor) {
		addInstruction(new StaticFieldInstruction(
				lowering.opcode() - SGET,
				indexOf(value),
				Types.instanceTypeFromInternalName(path.owner()),
				path.name(),
				new TypeParser(descriptor.literal()).requireClassType()
		));
	}

	@Override
	public void visitInvoke(@NotNull DalvikLowering lowering, RegisterOperands registers, MemberPath method,
	                        ASTIdentifier descriptor) {
		int opcode = lowering.opcode();
		List<Integer> registerValues = resolveRegisterOperands(registers);
		if (registerValues == null)
			return;

		ReferenceType owner = parseReferenceType(method.owner());
		MethodType methodType = parseMethodType(descriptor);
		updateOutRegisters(argumentWordCount(registerValues, registers.isRange()));
		if (registers.isRange()) {
			addInstruction(InvokeInstruction.range(
					opcode, owner, method.name(), methodType,
					registerValues.get(1) - registerValues.get(0) + 1, registerValues.get(0)));
		} else {
			addInstruction(new InvokeInstruction(opcode, owner, method.name(), methodType, toIntArray(registerValues)));
		}
	}

	@Override
	public void visitInvokeCustom(RegisterOperands registers, ASTIdentifier name, ASTIdentifier type, ASTElement handle, ASTArray arguments) {
		var bootstrapHandle = parseHandle(handle);
		MethodType methodType = parseMethodType(type);
		List<Constant> bootstrapArguments = parseConstants(arguments);
		List<Integer> registerValues = resolveRegisterOperands(registers);
		if (registerValues == null)
			return;

		updateOutRegisters(argumentWordCount(registerValues, registers.isRange()));
		if (registers.isRange()) {
			addInstruction(new InvokeCustomInstruction(
					bootstrapHandle,
					name.literal(),
					methodType,
					bootstrapArguments,
					registerValues.get(1) - registerValues.get(0) + 1,
					registerValues.get(0)
			));
			return;
		}
		addInstruction(new InvokeCustomInstruction(
				bootstrapHandle,
				name.literal(),
				methodType,
				bootstrapArguments,
				toIntArray(registerValues)
		));
	}

	@Override
	public void visitInvokePolymorphic(RegisterOperands registers, MemberPath method, ASTIdentifier descriptor,
	                                   ASTIdentifier proto) {
		List<Integer> registerValues = resolveRegisterOperands(registers);
		if (registerValues == null)
			return;

		ReferenceType owner = parseReferenceType(method.owner());
		MethodType methodType = parseMethodType(descriptor);
		MethodType callSiteType = parseMethodType(proto);
		updateOutRegisters(argumentWordCount(registerValues, registers.isRange()));
		if (registers.isRange()) {
			addInstruction(InvokeInstruction.polymorphicRange(
					owner, method.name(), methodType, callSiteType,
					registerValues.get(1) - registerValues.get(0) + 1, registerValues.get(0)));
		} else {
			addInstruction(InvokeInstruction.polymorphic(
					owner, method.name(), methodType, callSiteType, toIntArray(registerValues)));
		}
	}

	/**
	 * Allocates the slots of a register list or range and checks that they fit the fields of the encoding that
	 * carries them.
	 * <p>
	 * A named bound only gets its slot here, so the reversed-bounds check the resolver had to skip for named
	 * registers happens now that both bounds are known.
	 *
	 * @param registers
	 * 		Register operand of an invoke form or {@code filled-new-array}.
	 *
	 * @return Allocated slots in source order, or {@code null} after reporting a reversed range or a register the
	 * encoding cannot name. The declared register count is checked separately while the slots are allocated.
	 */
	private @Nullable List<Integer> resolveRegisterOperands(@NotNull RegisterOperands registers) {
		List<Integer> registerValues = indexOfAll(registers);
		List<RegisterRef> refs = registers.registers();
		if (registers.isRange()) {
			int first = registerValues.get(0);
			int last = registerValues.get(1);
			if (last < first) {
				sink.error(DiagnosticCode.OPERAND_SHAPE, "Range instruction register bounds are reversed",
						currentInstructionAst.location());
				return null;
			}
			return checkRange(refs.getFirst(), first, last) ? registerValues : null;
		}

		boolean fits = true;
		for (int i = 0; i < refs.size(); i++)
			fits &= checkRegisterField(refs.get(i), registerValues.get(i), FOUR_BITS);
		return fits ? registerValues : null;
	}

	/**
	 * Checks that a range can be named by its encoding: its count must fit the eight-bit count field, and it must end
	 * inside the register space the sixteen-bit first-register field can reach.
	 *
	 * @param first
	 * 		Operand holding the first register of the range, which locates the diagnostics.
	 * @param firstIndex
	 * 		Slot of the first register.
	 * @param lastIndex
	 * 		Slot of the last register, which is not below {@code firstIndex}.
	 *
	 * @return {@code true} when the range fits its encoding, otherwise {@code false} after reporting each limit it
	 * exceeds.
	 */
	private boolean checkRange(@NotNull RegisterRef first, int firstIndex, int lastIndex) {
		// Widened, so a run whose bounds approach the int range cannot wrap into a small, plausible count.
		long count = (long) lastIndex - firstIndex + 1;
		boolean fits = true;
		if (count > maxIndex(EIGHT_BITS)) {
			reportEncodingLimit(first, "Range of " + count + " registers does not fit the " + EIGHT_BITS
					+ "-bit count field of '" + mnemonic() + "', which takes at most " + maxIndex(EIGHT_BITS)
					+ " registers");
			fits = false;
		}
		if (lastIndex > maxIndex(SIXTEEN_BITS)) {
			reportEncodingLimit(first, "Range from v" + firstIndex + " to v" + lastIndex + " extends past v"
					+ maxIndex(SIXTEEN_BITS) + ", the highest register the " + SIXTEEN_BITS + "-bit range field of '"
					+ mnemonic() + "' can name");
			fits = false;
		}
		return fits;
	}

	/**
	 * Reports a literal that no single encoding of its operation can carry with these registers. The message names the
	 * ranges each form takes, so the author can change the literal or the registers without choosing a form.
	 *
	 * @param lowering
	 * 		Encodings of the operation.
	 * @param literal
	 * 		Literal operand that no encoding holds.
	 * @param toIndex
	 * 		Slot of the destination.
	 * @param fromIndex
	 * 		Slot of the source.
	 */
	private void reportNoLiteralEncoding(@NotNull ArithmeticLowering lowering, @NotNull IntegerLiteral literal,
	                                     int toIndex, int fromIndex) {
		List<String> forms = new ArrayList<>(2);
		if (lowering.literal8() != NONE)
			forms.add(literalForm(EIGHT_BITS, EIGHT_BITS));
		if (lowering.literal16() != NONE)
			forms.add(literalForm(SIXTEEN_BITS, FOUR_BITS));
		sink.add(new Diagnostic(Severity.ERROR, DiagnosticPhase.BACKEND_EMISSION, DiagnosticCode.TARGET_LIMIT,
				"No single '" + mnemonic() + "' instruction takes literal " + literal.value()
						+ " with registers v" + toIndex + " and v" + fromIndex + ": it takes "
						+ String.join(", or ", forms),
				operandLocation(literal), List.of()));
	}

	/**
	 * @param literalBits
	 * 		Width of the signed literal field of a form.
	 * @param registerBits
	 * 		Width of the register fields of the same form.
	 *
	 * @return Description of the literal range and the register range that one form accepts.
	 */
	private static @NotNull String literalForm(int literalBits, int registerBits) {
		int half = 1 << (literalBits - 1);
		return "a literal from " + -half + " through " + (half - 1) + " with registers v0 through v"
				+ maxIndex(registerBits);
	}

	/**
	 * @param value
	 * 		Literal value.
	 * @param bits
	 * 		Width of the signed field.
	 *
	 * @return {@code true} when {@code value} fits a two's-complement field of that width.
	 */
	private static boolean fitsSigned(int value, int bits) {
		int half = 1 << (bits - 1);
		return value >= -half && value < half;
	}

	/**
	 * @param index
	 * 		Slot to test.
	 * @param bits
	 * 		Width of the register field.
	 *
	 * @return {@code true} when {@code index} is a slot the field can hold.
	 */
	private static boolean fitsField(int index, int bits) {
		return index >= 0 && index <= maxIndex(bits);
	}

	/**
	 * @param ref
	 * 		Register operand the slot was allocated for.
	 * @param index
	 * 		Slot the operand resolved to.
	 * @param bits
	 * 		Width of the register field the instruction carries the slot in.
	 *
	 * @return {@code true} when the slot fits the field, otherwise {@code false} after reporting it.
	 */
	private boolean checkRegisterField(@NotNull RegisterRef ref, int index, int bits) {
		if (fitsField(index, bits))
			return true;

		String subject = ref.index() == null
				? "Register '" + ref.name() + "' (v" + index + ")"
				: "Register v" + index;
		reportEncodingLimit(ref, subject + " does not fit the " + bits + "-bit register field of '" + mnemonic()
				+ "', which names registers v0 through v" + maxIndex(bits));
		return false;
	}

	/**
	 * Reports an operand its instruction's encoding cannot carry. This is not the declared register count being
	 * exceeded, which {@link #ensureRegisterAllowed(int)} reports, so the two limits can be told apart.
	 *
	 * @param operand
	 * 		Register or literal operand that cannot be encoded.
	 * @param message
	 * 		Description of the limit that was exceeded.
	 */
	private void reportEncodingLimit(@NotNull OperandValue operand, @NotNull String message) {
		sink.add(new Diagnostic(Severity.ERROR, DiagnosticPhase.BACKEND_EMISSION, DiagnosticCode.TARGET_LIMIT,
				message, operandLocation(operand), List.of()));
	}

	/**
	 * @param operand
	 * 		Operand value of the instruction being emitted. A register may be one of a list or range.
	 *
	 * @return Location of the source operand holding the value, or of the instruction when no operand holds it.
	 */
	private @Nullable Location operandLocation(@NotNull OperandValue operand) {
		if (currentInstructionAst == null)
			return null;
		for (ValidatedOperand validated : currentInstructionAst.operands()) {
			if (validated.value() == operand
					|| (validated.value() instanceof RegisterOperands operands
					&& operands.registers().stream().anyMatch(register -> register == operand)))
				return validated.source().location();
		}
		return currentInstructionAst.location();
	}

	/**
	 * @return Source spelling of the instruction being emitted, such as {@code add-int}.
	 */
	private @NotNull String mnemonic() {
		return currentInstructionAst == null ? "instruction" : currentInstructionAst.source().identifier().literal();
	}

	/**
	 * @param bits
	 * 		Width of a register field.
	 *
	 * @return Highest slot a field of that width can hold.
	 */
	private static int maxIndex(int bits) {
		return (1 << bits) - 1;
	}

	/**
	 * Adds a {@code const} whose form holds the value and can name the destination.
	 *
	 * @param register
	 * 		Destination slot, which the caller has already checked against the eight-bit destination field.
	 * @param value
	 * 		Bits of the constant, kept exactly as written.
	 */
	private void addConst(int register, int value) {
		addInstruction(new ConstInstruction(constOpcode(register, value), register, value));
	}

	/**
	 * @param register
	 * 		Destination slot, which the caller has already checked against the eight-bit destination field.
	 * @param value
	 * 		Bits of the constant.
	 *
	 * @return The narrowest {@code const} form that holds the value and can name the destination. The four-bit
	 * destination of {@code const/4} limits it to {@code v0} through {@code v15}, so a small value in a higher register
	 * takes the sixteen-bit form instead.
	 */
	private static int constOpcode(int register, int value) {
		if (register <= maxIndex(FOUR_BITS) && value >= -8 && value <= 7) // 4 bit signed const
			return CONST_4;
		if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) // 16 bit signed const
			return CONST_16;
		if ((value & 0xFFFF) == 0) // bits are only set in the high 16 bits
			return CONST_HIGH16;
		return CONST;
	}

	@Override
	public void visitUnaryOperation(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from) {
		addInstruction(new UnaryInstruction(
				lowering.opcode(),
				indexOf(to),
				indexOf(from)
		));
	}

	@Override
	public void visitLabel(@NotNull ASTIdentifier label) {
		Label target = getLabel(label.literal());
		labelOrder.putIfAbsent(target, nextLabelOrder++);
		if (pendingLineNumber != Label.UNASSIGNED && target.lineNumber() == Label.UNASSIGNED) {
			target.lineNumber(pendingLineNumber);
			pendingLineNumber = Label.UNASSIGNED;
		}
		codeBuilder.add(target);
	}

	@Override
	public void visitLineNumber(@NotNull ASTNumber line) {
		pendingLineNumber = line.asInt();
	}

	@Override
	public void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type) {
		Label begin = getLabel(start.literal());
		Label finish = getLabel(end.literal());
		Label handlerTarget = getLabel(handler.literal());

		// The range is written as two labels, and its extent is their distance, so a label the body never
		// declares would be encoded at whatever position it happens to hold — an address that means nothing.
		// Reporting here keeps a typo in the exception table from becoming a try region over unrelated code.
		var entries = List.of(Map.entry("start", begin), Map.entry("end", finish), Map.entry("handler", handlerTarget));
		for (var entry : entries) {
			if (!labelOrder.containsKey(entry.getValue())) {
				sink.error(DiagnosticCode.MISSING_LABEL,
						"Exception " + entry.getKey() + " label is not declared in the method code",
						currentInstructionAst == null ? null : currentInstructionAst.location());
				return;
			}
		}

		// An empty or reversed range would encode a try region that covers nothing or has a negative length,
		// which is an authoring mistake rather than a program the runtime can act on.
		if (labelOrder.get(finish) <= labelOrder.get(begin)) {
			sink.error(DiagnosticCode.OPERAND_SHAPE,
					"Exception range end label must come after its start label",
					currentInstructionAst == null ? null : currentInstructionAst.location());
			return;
		}
		Handler parsedHandler = new Handler(
				handlerTarget,
				"*".equals(type.literal()) ? null : parseInstanceType(type)
		);
		for (int i = 0; i < tryCatches.size(); i++) {
			TryCatch existing = tryCatches.get(i);
			if (existing.begin() != begin || existing.end() != finish) {
				continue;
			}
			List<Handler> handlers = new ArrayList<>(existing.handlers());
			handlers.add(parsedHandler);
			tryCatches.set(i, new TryCatch(begin, finish, List.copyOf(handlers)));
			return;
		}
		tryCatches.add(new TryCatch(begin, finish, List.of(parsedHandler)));
	}

	@Override
	public void visitEnd() {
		if (pendingLineNumber != Label.UNASSIGNED) {
			Label label = new Label();
			label.lineNumber(pendingLineNumber);
			codeBuilder.add(label);
			pendingLineNumber = Label.UNASSIGNED;
		}
	}

	/**
	 * @param registers
	 * 		Allocated register slots.
	 *
	 * @return The same slots as a primitive array, which is what the dex instruction constructors take.
	 */
	private static int[] toIntArray(@NotNull List<Integer> registers) {
		int[] values = new int[registers.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = registers.get(i);
		}
		return values;
	}
}
