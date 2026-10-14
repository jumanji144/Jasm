package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.AnalysisWorklist;
import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.descriptor.Descriptors;
import me.darknet.assembler.descriptor.PrimitiveType;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
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
import me.darknet.dex.tree.definitions.instructions.Invoke;
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
import me.darknet.dex.tree.type.MethodType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves what kind of constant type we should print some value of {@code const} and {@code const-wide} with.
 * <p>
 * Dalvik constants only carry raw bits, so {@code const} may hold an {@code int} or a {@code float} and
 * {@code const-wide} may hold a {@code long} or a {@code double}. We track use cases of the values to infer
 * the most likely type. If the uses are inconsistent, we return {@link ResolvedType#UNKNOWN}.
 * <p>
 * When the type is unknown we fall back to printing the constant as an integer or long, which is always valid,
 * and arguably the most common and readable representation.
 */
public final class DalvikConstantTypeResolver {
	/**
	 * Category of a resolved Dalvik constant.
	 */
	public enum ResolvedType {
		INT,
		FLOAT,
		LONG,
		DOUBLE,
		/** Cannot determine specific category from uses. */
		UNKNOWN
	}

	private static final int INT_VOTE = 1 << PrimitiveType.INT.ordinal();
	private static final int FLOAT_VOTE = 1 << PrimitiveType.FLOAT.ordinal();
	private static final int LONG_VOTE = 1 << PrimitiveType.LONG.ordinal();
	private static final int DOUBLE_VOTE = 1 << PrimitiveType.DOUBLE.ordinal();

	private final MethodMember method;
	private final Code code;
	private final List<Instruction> instructions;
	private final int registers;
	private final BitSet wideOrigins;
	private final BitSet[] constantOrigins;

	private DalvikConstantTypeResolver(@NotNull MethodMember method, @NotNull Code code,
	                                   @NotNull BitSet wideOrigins, @NotNull BitSet[] constantOrigins) {
		this.method = method;
		this.code = code;
		this.instructions = code.getInstructions();
		this.registers = code.getRegisters();
		this.wideOrigins = wideOrigins;
		this.constantOrigins = constantOrigins;
	}

	/**
	 * @param method
	 * 		Method declaring the code.
	 * @param code
	 * 		Decoded method code.
	 *
	 * @return Identity map from every constant instruction to its consistent primitive category, or
	 * {@link ResolvedType#UNKNOWN} when the uses provide no unique category. Unreachable constants are included.
	 */
	public static @NotNull Map<Instruction, ResolvedType> resolve(@NotNull MethodMember method, @NotNull Code code) {
		List<Instruction> instructions = code.getInstructions();

		// Track which instructions are constants and which are wide constants.
		BitSet wideOrigins = new BitSet();
		BitSet[] constantOrigins = new BitSet[instructions.size()];
		boolean hasConstants = false;
		for (int index = 0; index < instructions.size(); index++) {
			Instruction instruction = instructions.get(index);
			if (instruction instanceof ConstWideInstruction)
				wideOrigins.set(index);
			else if (!(instruction instanceof ConstInstruction))
				continue;

			// Set the bit for this instruction in its own origin set, so we can filter votes to matching widths.
			BitSet origin = new BitSet(index + 1);
			origin.set(index);
			constantOrigins[index] = origin;
			hasConstants = true;
		}
		if (!hasConstants)
			return Map.of();

		// Run the analysis to a fix-point and replay every reachable instruction once on its converged input to collect votes.
		int[] votes = new DalvikConstantTypeResolver(method, code, wideOrigins, constantOrigins).collectVotes();

		// Map each constant instruction to its resolved type based on the votes.
		Map<Instruction, ResolvedType> types = new IdentityHashMap<>();
		for (int index = 0; index < instructions.size(); index++) {
			Instruction instruction = instructions.get(index);
			int mask = votes[index];
			if (instruction instanceof ConstInstruction) {
				ResolvedType type = mask == FLOAT_VOTE ? ResolvedType.FLOAT : mask == INT_VOTE ? ResolvedType.INT : ResolvedType.UNKNOWN;
				types.put(instruction, type);
			} else if (instruction instanceof ConstWideInstruction) {
				ResolvedType type = mask == DOUBLE_VOTE ? ResolvedType.DOUBLE : mask == LONG_VOTE ? ResolvedType.LONG : ResolvedType.UNKNOWN;
				types.put(instruction, type);
			}
		}
		return Collections.unmodifiableMap(types);
	}

	/**
	 * Runs the origin analysis to a fix-point.
	 *
	 * @return Per-instruction bitmask of {@code 1 << PrimitiveType.ordinal()} votes.
	 */
	private int @NotNull [] collectVotes() {
		int[] votes = new int[instructions.size()];

		// Build a control flow graph of the method code to iterate over reachable instructions.
		DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(code, Map.of());
		Integer entryIndex = graph.entryIndex();
		if (entryIndex == null)
			return votes;

		// Set up the worklist and input frames for the analysis.
		// The entry frame seeds array types of incoming parameters.
		Frame[] inputs = new Frame[instructions.size()];
		inputs[entryIndex] = entryFrame();
		AnalysisWorklist worklist = new AnalysisWorklist();
		Set<Integer> queued = new HashSet<>();
		worklist.add(entryIndex);
		queued.add(entryIndex);

		// Iterate over the worklist until all reachable instructions have been processed to a fix-point.
		AnalysisWorklist.Entry work;
		while ((work = worklist.next()) != null) {
			int index = work.index();
			queued.remove(index);

			// Skip if the instruction is unreachable (no input frame).
			Frame input = inputs[index];
			if (input == null)
				continue;

			// Apply the instruction to the input frame to produce an output frame, and propagate it to successors.
			Frame output = input.copy();
			transfer(output, instructions.get(index), index, null);
			for (int successor : graph.normalSuccessors().getOrDefault(index, List.of()))
				propagate(successor, output, inputs, worklist, queued);
			for (DalvikControlFlowGraph.ExceptionEdge edge : graph.exceptionalSuccessors().getOrDefault(index, List.of())) {
				Frame exceptional = input.copy();
				exceptional.pendingArrayType = null;
				propagate(edge.targetIndex(), exceptional, inputs, worklist, queued);
			}
		}

		// Replay every reachable instruction once on its converged input to collect votes for constant types.
		for (int index = 0; index < inputs.length; index++)
			if (inputs[index] != null)
				transfer(inputs[index].copy(), instructions.get(index), index, votes);
		return votes;
	}

	/**
	 * Propagates {@code incoming} to the input frame of {@code target}.
	 * If the input frame changes, {@code target} is added to the worklist.
	 *
	 * @param target
	 * 		Target instruction index to propagate to.
	 * @param incoming
	 * 		Frame to propagate.
	 * @param inputs
	 * 		Array of input frames for each instruction.
	 * @param worklist
	 * 		Worklist of instructions to process.
	 * @param queued
	 * 		Set of instruction indices currently in the worklist.
	 */
	private static void propagate(int target, @NotNull Frame incoming, Frame @NotNull [] inputs,
	                              @NotNull  AnalysisWorklist worklist, @NotNull   Set<Integer> queued) {
		Frame existing = inputs[target];
		boolean changed;
		if (existing == null) {
			inputs[target] = incoming.copy();
			changed = true;
		} else {
			changed = existing.merge(incoming);
		}
		if (changed && queued.add(target))
			worklist.add(target);
	}

	/**
	 * @return Frame with array types of incoming parameters, or empty if the register layout does not match the descriptor.
	 */
	private @NotNull Frame entryFrame() {
		Frame frame = new Frame(registers);
		boolean isStatic = (method.getAccess() & DalvikModifiers.ACC_STATIC) != 0;
		List<ClassType> parameters = method.getType().parameterTypes();
		int words = isStatic ? 0 : 1;
		for (ClassType parameter : parameters)
			words += Descriptors.isWideType(parameter.descriptor()) ? 2 : 1;
		if (words != code.getIn() || words > registers)
			return frame;

		int register = registers - words + (isStatic ? 0 : 1);
		for (ClassType parameter : parameters) {
			String descriptor = parameter.descriptor();
			frame.arrayTypes[register] = Descriptors.arrayDescriptor(descriptor);
			register += Descriptors.isWideType(descriptor) ? 2 : 1;
		}
		return frame;
	}

	/**
	 * Applies one instruction to {@code frame}. Reads and votes happen before writes so an instruction that
	 * overwrites one of its operands still votes on the operand's incoming value.
	 *
	 * @param frame
	 * 		Frame to apply the instruction to. This frame is mutated in place.
	 * @param instruction
	 * 		Instruction to apply.
	 * @param index
	 * 		Index of the instruction in the method code.
	 * @param votes
	 * 		Vote accumulator, or {@code null} while iterating to the fix-point.
	 */
	private void transfer(Frame frame, Instruction instruction, int index, int @Nullable [] votes) {
		String pendingArrayType = null;
		switch (instruction) {
			case ConstInstruction constant -> writeNarrow(frame, constant.register(), constantOrigins[index], null);
			case ConstWideInstruction constant -> writeWide(frame, constant.register(), constantOrigins[index]);
			case ConstStringInstruction constant -> writeNarrow(frame, constant.register(), null, null);
			case ConstTypeInstruction constant -> writeNarrow(frame, constant.register(), null, null);
			case ConstMethodHandleInstruction constant -> writeNarrow(frame, constant.destination(), null, null);
			case ConstMethodTypeInstruction constant -> writeNarrow(frame, constant.destination(), null, null);
			case NewInstanceInstruction newInstance -> writeNarrow(frame, newInstance.dest(), null, null);
			case InstanceOfInstruction instanceOf -> writeNarrow(frame, instanceOf.destination(), null, null);
			case ArrayLengthInstruction arrayLength -> writeNarrow(frame, arrayLength.dest(), null, null);
			case MoveExceptionInstruction moveException -> writeNarrow(frame, moveException.register(), null, null);
			case MoveInstruction move -> writeNarrow(frame, move.to(), frame.origins(move.from()), frame.arrayType(move.from()));
			case MoveObjectInstruction move -> writeNarrow(frame, move.to(), frame.origins(move.from()), frame.arrayType(move.from()));
			case MoveWideInstruction move -> writeWide(frame, move.to(), frame.origins(move.from()));
			case MoveResultInstruction moveResult -> {
				switch (moveResult.type()) {
					case Result.OBJECT -> writeNarrow(frame, moveResult.to(), null, frame.pendingArrayType);
					case Result.WIDE -> writeWide(frame, moveResult.to(), null);
					default -> writeNarrow(frame, moveResult.to(), null, null);
				}
			}
			case ReturnInstruction ret -> {
				if (ret.type() != Return.VOID)
					vote(frame, ret.register(), Descriptors.primitiveCategory(method.getType().returnType().descriptor()), votes);
			}
			case UnaryInstruction unary -> {
				vote(frame, unary.source(), unarySourceCategory(unary.opcode()), votes);
				writeValue(frame, unary.dest(), isWideUnaryResult(unary.opcode()));
			}
			case BinaryInstruction binary -> {
				PrimitiveType category = binaryCategory(binary.opcode() - Opcodes.ADD_INT);
				vote(frame, binary.a(), category, votes);
				vote(frame, binary.b(), binaryRightCategory(binary.opcode() - Opcodes.ADD_INT, category), votes);
				writeValue(frame, binary.dest(), Descriptors.isWideType(category));
			}
			case Binary2AddrInstruction binary -> {
				PrimitiveType category = binaryCategory(binary.opcode() - Opcodes.ADD_INT_2ADDR);
				vote(frame, binary.a(), category, votes);
				vote(frame, binary.b(), binaryRightCategory(binary.opcode() - Opcodes.ADD_INT_2ADDR, category), votes);
				writeValue(frame, binary.a(), Descriptors.isWideType(category));
			}
			case BinaryLiteralInstruction binary -> {
				vote(frame, binary.src(), PrimitiveType.INT, votes);
				writeNarrow(frame, binary.dest(), null, null);
			}
			case CompareInstruction compare -> {
				PrimitiveType category = switch (compare.opcode()) {
					case Opcodes.CMPL_FLOAT, Opcodes.CMPG_FLOAT -> PrimitiveType.FLOAT;
					case Opcodes.CMPL_DOUBLE, Opcodes.CMPG_DOUBLE -> PrimitiveType.DOUBLE;
					case Opcodes.CMP_LONG -> PrimitiveType.LONG;
					default -> null;
				};
				vote(frame, compare.a(), category, votes);
				vote(frame, compare.b(), category, votes);
				writeNarrow(frame, compare.dest(), null, null);
			}
			case BranchInstruction branch -> {
				// if-eq and if-ne may compare references, so only ordering tests imply int operands.
				if (branch.opcode() >= Opcodes.IF_LT && branch.opcode() <= Opcodes.IF_LE) {
					vote(frame, branch.a(), PrimitiveType.INT, votes);
					vote(frame, branch.b(), PrimitiveType.INT, votes);
				}
			}
			case BranchZeroInstruction branch -> {
				if (branch.opcode() >= Opcodes.IF_LTZ && branch.opcode() <= Opcodes.IF_LEZ)
					vote(frame, branch.a(), PrimitiveType.INT, votes);
			}
			case ArrayInstruction array -> transferArray(frame, array, votes);
			case InstanceFieldInstruction field -> transferField(frame, field.opcode() - Opcodes.IGET, field.value(), field.type(), votes);
			case StaticFieldInstruction field -> transferField(frame, field.opcode() - Opcodes.SGET, field.value(), field.type(), votes);
			case InvokeInstruction invoke -> {
				if (votes != null) {
					int skip = invoke.opcode() == Invoke.STATIC ? 0 : 1;
					if (invoke.isRange())
						voteRangeArguments(frame, invoke.first(), invoke.last(), skip, invoke.type(), votes);
					else
						voteArguments(frame, invoke.arguments(), skip, invoke.type(), votes);
				}
				pendingArrayType = Descriptors.arrayDescriptor(invoke.type().returnType().descriptor());
			}
			case InvokeCustomInstruction invoke -> {
				if (votes != null) {
					if (invoke.isRange())
						voteRangeArguments(frame, invoke.first(), invoke.last(), 0, invoke.type(), votes);
					else
						voteArguments(frame, invoke.argumentRegisters(), 0, invoke.type(), votes);
				}
				pendingArrayType = Descriptors.arrayDescriptor(invoke.type().returnType().descriptor());
			}
			case FilledNewArrayInstruction filled -> {
				String arrayType = filled.componentType().descriptor();
				PrimitiveType category = Descriptors.primitiveCategory(arrayType.substring(1));
				if (votes != null && category != null) {
					int[] registers = filled.registers();
					if (filled.isRange()) {
						for (int register = filled.first(); register <= filled.last(); register++)
							vote(frame, register, category, votes);
					} else if (registers != null) {
						for (int register : registers)
							vote(frame, register, category, votes);
					}
				}
				pendingArrayType = arrayType;
			}
			case NewArrayInstruction newArray -> {
				vote(frame, newArray.sizeRegister(), PrimitiveType.INT, votes);
				writeNarrow(frame, newArray.dest(), null, newArray.componentType().descriptor());
			}
			case PackedSwitchInstruction packedSwitch -> vote(frame, packedSwitch.register(), PrimitiveType.INT, votes);
			case SparseSwitchInstruction sparseSwitch -> vote(frame, sparseSwitch.register(), PrimitiveType.INT, votes);
			case CheckCastInstruction checkCast -> {
				if (inRange(checkCast.register()))
					frame.arrayTypes[checkCast.register()] = Descriptors.arrayDescriptor(checkCast.type().descriptor());
			}
			case MonitorInstruction ignored -> {}
			case ThrowInstruction ignored -> {}
			case FillArrayDataInstruction ignored -> {}
			case GotoInstruction ignored -> {}
			case NopInstruction ignored -> {}
			case Label ignored -> {}
		}
		frame.pendingArrayType = pendingArrayType;
	}

	/**
	 * Votes on the index and value of an array instruction, based on the array's type.
	 *
	 * @param frame
	 * 		Frame to read from and write to.
	 * @param array
	 * 		Array instruction to process.
	 * @param votes
	 * 		Vote accumulator, or {@code null} while iterating to the fix-point.
	 */
	private void transferArray(@NotNull Frame frame, @NotNull ArrayInstruction array, int @Nullable [] votes) {
		vote(frame, array.index(), PrimitiveType.INT, votes);
		String arrayType = frame.arrayType(array.array());
		switch (array.opcode()) {
			case Opcodes.APUT -> {
				if ("[I".equals(arrayType))
					vote(frame, array.value(), PrimitiveType.INT, votes);
				else if ("[F".equals(arrayType))
					vote(frame, array.value(), PrimitiveType.FLOAT, votes);
			}
			case Opcodes.APUT_WIDE -> {
				if ("[J".equals(arrayType))
					vote(frame, array.value(), PrimitiveType.LONG, votes);
				else if ("[D".equals(arrayType))
					vote(frame, array.value(), PrimitiveType.DOUBLE, votes);
			}
			case Opcodes.APUT_BOOLEAN, Opcodes.APUT_BYTE, Opcodes.APUT_CHAR, Opcodes.APUT_SHORT -> vote(frame, array.value(), PrimitiveType.INT, votes);
			case Opcodes.APUT_OBJECT -> {}
			case Opcodes.AGET_WIDE -> writeWide(frame, array.value(), null);
			case Opcodes.AGET_OBJECT -> writeNarrow(frame, array.value(), null, arrayType != null && arrayType.startsWith("[[") ? arrayType.substring(1) : null);
			default -> writeNarrow(frame, array.value(), null, null);
		}
	}

	/**
	 * Votes on the value of a field instruction, based on the field's type.
	 *
	 * @param frame
	 * 		Frame to read from and write to.
	 * @param kind
	 * 		Kind of field instruction, as an offset from {@code IGET} or {@code IPUT}.
	 * @param value
	 * 		Value register of the field instruction.
	 * @param type
	 * 		Type of the field being accessed.
	 * @param votes
	 * 		Vote accumulator, or {@code null} while iterating to the fix-point.
	 */
	private void transferField(@NotNull Frame frame, int kind, int value, ClassType type, int @Nullable [] votes) {
		int putKind = Opcodes.IPUT - Opcodes.IGET;
		if (kind >= putKind) {
			vote(frame, value, Descriptors.primitiveCategory(type.descriptor()), votes);
			return;
		}
		switch (kind) {
			case Opcodes.IGET_WIDE - Opcodes.IGET -> writeWide(frame, value, null);
			case Opcodes.IGET_OBJECT - Opcodes.IGET -> writeNarrow(frame, value, null, Descriptors.arrayDescriptor(type.descriptor()));
			default -> writeNarrow(frame, value, null, null);
		}
	}

	/**
	 * Votes on the argument registers of an invoke instruction, based on the method's parameter types.
	 *
	 * @param frame
	 * 		Frame to read from and write to.
	 * @param arguments
	 * 		Argument registers of the invoke instruction, or {@code null} for a range invoke.
	 * @param skip
	 * 		Number of leading arguments to skip (the receiver for non-static methods).
	 * @param type
	 * 		Method type of the invoked method.
	 * @param votes
	 * 		Vote accumulator, or {@code null} while iterating to the fix-point.
	 */
	private void voteArguments(@NotNull Frame frame, int @Nullable [] arguments, int skip, @NotNull MethodType type, int[] votes) {
		if (arguments == null)
			return;
		int position = skip;
		for (ClassType parameter : type.parameterTypes()) {
			if (position >= arguments.length)
				return;
			String descriptor = parameter.descriptor();
			vote(frame, arguments[position], Descriptors.primitiveCategory(descriptor), votes);
			position += Descriptors.isWideType(descriptor) ? 2 : 1;
		}
	}

	/**
	 * Votes on the argument registers of a range invoke instruction, based on the method's parameter types.
	 *
	 * @param frame
	 * 		Frame to read from and write to.
	 * @param first
	 * 		First register of the range invoke instruction.
	 * @param last
	 * 		Last register of the range invoke instruction.
	 * @param skip
	 * 		Number of leading arguments to skip (the receiver for non-static methods).
	 * @param type
	 * 		Method type of the invoked method.
	 * @param votes
	 * 		Vote accumulator, or {@code null} while iterating to the fix-point.
	 */
	private void voteRangeArguments(@NotNull Frame frame, int first, int last, int skip, @NotNull MethodType type, int[] votes) {
		int register = first + skip;
		for (ClassType parameter : type.parameterTypes()) {
			if (register > last)
				return;
			String descriptor = parameter.descriptor();
			vote(frame, register, Descriptors.primitiveCategory(descriptor), votes);
			register += Descriptors.isWideType(descriptor) ? 2 : 1;
		}
	}

	/**
	 * Votes on the value of a register, based on the given category.
	 *
	 * @param frame
	 * 		Frame to read from.
	 * @param register
	 * 		Register to vote on.
	 * @param category
	 * 		Category of the value in the register, or {@code null} if unknown.
	 * @param votes
	 * 		Vote accumulator, or {@code null} while iterating to the fix-point.
	 */
	private void vote(@NotNull Frame frame, int register, @Nullable PrimitiveType category, int @Nullable [] votes) {
		if (votes == null || category == null)
			return;

		BitSet origins = frame.origins(register);
		if (origins == null)
			return;

		boolean wide = Descriptors.isWideType(category);
		int bit = 1 << category.ordinal();
		for (int origin = origins.nextSetBit(0); origin >= 0; origin = origins.nextSetBit(origin + 1))
			if (wideOrigins.get(origin) == wide)
				votes[origin] |= bit;
	}

	/**
	 * Writes a value to a register, breaking any wide pair that may have been there.
	 *
	 * @param frame
	 * 		Frame to write to.
	 * @param register
	 * 		Register to write to.
	 * @param wide
	 * 		Whether the value is wide (long or double).
	 */
	private void writeValue(@NotNull Frame frame, int register, boolean wide) {
		if (wide)
			writeWide(frame, register, null);
		else
			writeNarrow(frame, register, null, null);
	}

	/**
	 * Writes a narrow value to a register, breaking any wide pair that may have been there.
	 *
	 * @param frame
	 * 		Frame to write to.
	 * @param register
	 * 		Register to write to.
	 * @param origins
	 * 		Origins of the value being written, or {@code null} if unknown.
	 * @param arrayType
	 * 		Array type of the value being written, or {@code null} if not an array or unknown.
	 */
	private void writeNarrow(Frame frame, int register, @Nullable BitSet origins, @Nullable String arrayType) {
		if (!inRange(register))
			return;

		frame.origins[register] = origins;
		frame.arrayTypes[register] = arrayType;

		breakWidePair(frame, register - 1);
	}

	/**
	 * Writes a wide value to a register, breaking any wide pair that may have been there.
	 *
	 * @param frame
	 * 		Frame to write to.
	 * @param register
	 * 		Register to write to.
	 * @param origins
	 * 		Origins of the value being written, or {@code null} if unknown.
	 */
	private void writeWide(Frame frame, int register, @Nullable BitSet origins) {
		if (!inRange(register))
			return;

		frame.origins[register] = origins;
		frame.arrayTypes[register] = null;

		if (inRange(register + 1)) {
			frame.origins[register + 1] = null;
			frame.arrayTypes[register + 1] = null;
		}

		breakWidePair(frame, register - 1);
	}

	/**
	 * Breaks a wide pair at {@code register} if it exists, by removing any wide origins from the register's origin set.
	 *
	 * @param frame
	 * 		Frame to modify.
	 * @param register
	 * 		Register to break the wide pair at.
	 */
	private void breakWidePair(@NotNull Frame frame, int register) {
		if (!inRange(register))
			return;

		BitSet origins = frame.origins[register];
		if (origins == null || !origins.intersects(wideOrigins))
			return;

		BitSet remaining = (BitSet) origins.clone();
		remaining.andNot(wideOrigins);
		frame.origins[register] = remaining.isEmpty() ? null : remaining;
	}

	/**
	 * @param register
	 * 		Register to check.
	 *
	 * @return {@code true} if the register is in range of the method's registers, {@code false} otherwise.
	 */
	private boolean inRange(int register) {
		return register >= 0 && register < registers;
	}

	/**
	 * @param offset
	 * 		Offset of a binary instruction opcode from {@code ADD_INT}.
	 *
	 * @return Category of the binary instruction, or {@code null} if the instruction is not a binary instruction.
	 */
	private static @Nullable PrimitiveType binaryCategory(int offset) {
		if (offset < 0)
			return null;
		if (offset <= Opcodes.USHR_INT - Opcodes.ADD_INT)
			return PrimitiveType.INT;
		if (offset <= Opcodes.USHR_LONG - Opcodes.ADD_INT)
			return PrimitiveType.LONG;
		if (offset <= Opcodes.REM_FLOAT - Opcodes.ADD_INT)
			return PrimitiveType.FLOAT;
		if (offset <= Opcodes.REM_DOUBLE - Opcodes.ADD_INT)
			return PrimitiveType.DOUBLE;
		return null;
	}

	/**
	 * @param offset
	 * 		Offset of a binary instruction opcode from {@code ADD_INT}.
	 * @param category
	 * 		Category of the left operand of the binary instruction, or {@code null} if unknown.
	 *
	 * @return Category of the right operand of the binary instruction, or {@code null} if unknown.
	 */
	private static @Nullable PrimitiveType binaryRightCategory(int offset, @Nullable PrimitiveType category) {
		return switch (offset + Opcodes.ADD_INT) {
			case Opcodes.SHL_LONG, Opcodes.SHR_LONG, Opcodes.USHR_LONG -> PrimitiveType.INT;
			default -> category;
		};
	}

	/**
	 * @param opcode
	 * 		Opcode of a unary instruction.
	 *
	 * @return Category of the source operand of the unary instruction, or {@code null} if the instruction is not a unary instruction.
	 */
	private static @Nullable PrimitiveType unarySourceCategory(int opcode) {
		return switch (opcode) {
			case Opcodes.NEG_INT,
			     Opcodes.NOT_INT,
			     Opcodes.INT_TO_LONG,
			     Opcodes.INT_TO_FLOAT,
			     Opcodes.INT_TO_DOUBLE,
			     Opcodes.INT_TO_BYTE,
			     Opcodes.INT_TO_CHAR,
			     Opcodes.INT_TO_SHORT -> PrimitiveType.INT;
			case Opcodes.NEG_LONG,
			     Opcodes.NOT_LONG,
			     Opcodes.LONG_TO_INT,
			     Opcodes.LONG_TO_FLOAT,
			     Opcodes.LONG_TO_DOUBLE -> PrimitiveType.LONG;
			case Opcodes.NEG_FLOAT,
			     Opcodes.FLOAT_TO_INT,
			     Opcodes.FLOAT_TO_LONG,
			     Opcodes.FLOAT_TO_DOUBLE -> PrimitiveType.FLOAT;
			case Opcodes.NEG_DOUBLE,
			     Opcodes.DOUBLE_TO_INT,
			     Opcodes.DOUBLE_TO_LONG,
			     Opcodes.DOUBLE_TO_FLOAT -> PrimitiveType.DOUBLE;
			default -> null;
		};
	}

	/**
	 * @param opcode
	 * 		Opcode of a unary instruction.
	 *
	 * @return {@code true} if the result of the unary instruction is wide (long or double), {@code false} otherwise.
	 */
	private static boolean isWideUnaryResult(int opcode) {
		return switch (opcode) {
			case Opcodes.NEG_LONG,
			     Opcodes.NOT_LONG,
			     Opcodes.NEG_DOUBLE,
			     Opcodes.INT_TO_LONG,
			     Opcodes.INT_TO_DOUBLE,
			     Opcodes.LONG_TO_DOUBLE,
			     Opcodes.FLOAT_TO_LONG,
			     Opcodes.FLOAT_TO_DOUBLE,
			     Opcodes.DOUBLE_TO_LONG -> true;
			default -> false;
		};
	}

	/**
	 * Frame of the origin analysis, tracking the origins and array types of each register.
	 */
	private static final class Frame {
		private final BitSet[] origins;
		private final String[] arrayTypes;
		private String pendingArrayType;

		/**
		 * @param registers
		 * 		Number of registers in the method code.
		 */
		private Frame(int registers) {
			this.origins = new BitSet[registers];
			this.arrayTypes = new String[registers];
		}

		/**
		 * Copy constructor.
		 *
		 * @param other
		 * 		Frame to copy.
		 */
		private Frame(@NotNull Frame other) {
			this.origins = other.origins.clone();
			this.arrayTypes = other.arrayTypes.clone();
			this.pendingArrayType = other.pendingArrayType;
		}

		/**
		 * @return Copy of this frame.
		 */
		private @NotNull Frame copy() {
			return new Frame(this);
		}

		/**
		 * @param register
		 * 		Register to get the origins of.
		 *
		 * @return Origins of the value in the register, or {@code null} if unknown.
		 */
		private @Nullable BitSet origins(int register) {
			return register >= 0 && register < origins.length ? origins[register] : null;
		}

		/**
		 * @param register
		 * 		Register to get the array type of.
		 *
		 * @return Array type of the value in the register, or {@code null} if not an array or unknown.
		 */
		private @Nullable String arrayType(int register) {
			return register >= 0 && register < arrayTypes.length ? arrayTypes[register] : null;
		}

		/**
		 * Merges the incoming frame into this frame.
		 *
		 * @param incoming
		 * 		Frame to merge into this frame.
		 *
		 * @return {@code true} if this frame changed as a result of the merge, {@code false} otherwise.
		 */
		private boolean merge(@NotNull Frame incoming) {
			boolean changed = false;
			for (int register = 0; register < origins.length; register++) {
				BitSet mine = origins[register];
				BitSet theirs = incoming.origins[register];
				if (theirs != null && !theirs.isEmpty() && mine != theirs) {
					if (mine == null) {
						origins[register] = theirs;
						changed = true;
					} else if (!containsAll(mine, theirs)) {
						BitSet union = (BitSet) mine.clone();
						union.or(theirs);
						origins[register] = union;
						changed = true;
					}
				}
				String arrayType = arrayTypes[register];
				if (arrayType != null && !arrayType.equals(incoming.arrayTypes[register])) {
					arrayTypes[register] = null;
					changed = true;
				}
			}
			if (pendingArrayType != null && !pendingArrayType.equals(incoming.pendingArrayType)) {
				pendingArrayType = null;
				changed = true;
			}
			return changed;
		}

		/**
		 * @param set
		 * 		Bit set to check.
		 * @param subset
		 * 		Bit set to check for being a subset of {@code set}.
		 *
		 * @return {@code true} if {@code subset} is a subset of {@code set}, {@code false} otherwise.
		 */
		private static boolean containsAll(@NotNull BitSet set, @NotNull BitSet subset) {
			for (int bit = subset.nextSetBit(0); bit >= 0; bit = subset.nextSetBit(bit + 1))
				if (!set.get(bit))
					return false;
			return true;
		}
	}
}
