package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.dex.file.instructions.Opcodes;
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
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import me.darknet.dex.tree.definitions.instructions.SparseSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.StaticFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.ThrowInstruction;
import me.darknet.dex.tree.definitions.instructions.UnaryInstruction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Computes register-word liveness for Dalvik code.
 */
final class DalvikRegisterLiveness {
	private DalvikRegisterLiveness() {}

	/**
	 * Creates a lazy lookup for the live-in registers of a method.
	 *
	 * @param code
	 * 		Method code whose register accesses are analyzed.
	 * @param graph
	 * 		Control-flow graph for {@code code}.
	 *
	 * @return Lookup that computes and caches live-in sets on first use.
	 */
	static @NotNull Lookup lookup(@NotNull Code code, @NotNull DalvikControlFlowGraph graph) {
		return new Lookup(code, graph);
	}

	/**
	 * Computes the registers live before every instruction using a backward fixed-point analysis.
	 *
	 * @param code
	 * 		Method code whose register accesses are analyzed.
	 * @param graph
	 * 		Control-flow graph for {@code code}.
	 *
	 * @return One live-in bit set per code instruction, including labels and unreachable instructions.
	 */
	private static @NotNull BitSet[] computeLiveIn(@NotNull Code code, @NotNull DalvikControlFlowGraph graph) {
		List<Instruction> instructions = code.getInstructions();
		int count = instructions.size();
		int registerCount = code.getRegisters();
		Effects[] effects = new Effects[count];
		List<List<Integer>> predecessors = new ArrayList<>(count);

		// Fill in the register effects of each instruction and initialize the predecessor lists.
		for (int index = 0; index < count; index++) {
			effects[index] = effects(instructions.get(index), registerCount);
			predecessors.add(new ArrayList<>());
		}

		// Revisit predecessors when a successor's live-in set changes.
		for (int index = 0; index < count; index++) {
			for (int successor : graph.normalSuccessors().getOrDefault(index, List.of()))
				predecessors.get(successor).add(index);
			for (DalvikControlFlowGraph.ExceptionEdge edge : graph.exceptionalSuccessors().getOrDefault(index, List.of()))
				predecessors.get(edge.targetIndex()).add(index);
		}

		BitSet[] liveIn = new BitSet[count];
		ArrayDeque<Integer> worklist = new ArrayDeque<>();
		boolean[] queued = new boolean[count];

		// Start in reverse instruction order so straight-line code approaches its fixed point in one pass.
		for (int index = 0; index < count; index++) {
			liveIn[index] = new BitSet(registerCount);
			worklist.addLast(count - index - 1);
			queued[count - index - 1] = true;
		}

		// Iterate until reaching a fixed point where no instruction's live-in set changes.
		while (!worklist.isEmpty()) {
			int index = worklist.removeFirst();
			queued[index] = false;
			Effects instructionEffects = effects[index];
			BitSet next = (BitSet) instructionEffects.uses().clone();

			// Normal successors receive the post-instruction frame, so normal definitions kill their live values.
			for (int successor : graph.normalSuccessors().getOrDefault(index, List.of())) {
				BitSet successorLive = (BitSet) liveIn[successor].clone();
				successorLive.andNot(instructionEffects.definitions());
				next.or(successorLive);
			}

			// Exceptional edges receive the pre-instruction frame, so normal definitions do not kill their live values.
			for (DalvikControlFlowGraph.ExceptionEdge edge : graph.exceptionalSuccessors().getOrDefault(index, List.of()))
				next.or(liveIn[edge.targetIndex()]);

			// If the live-in set changed, revisit all predecessors to propagate the change.
			if (!next.equals(liveIn[index])) {
				liveIn[index] = next;
				for (int predecessor : predecessors.get(index))
					if (!queued[predecessor]) {
						worklist.addLast(predecessor);
						queued[predecessor] = true;
					}
			}
		}
		return liveIn;
	}

	/**
	 * Describes the register reads and writes of one Dalvik instruction.
	 *
	 * @param instruction
	 * 		Instruction whose effects are described.
	 * @param registerCount
	 * 		Number of register words in the method.
	 *
	 * @return Register reads and definitions for {@code instruction}.
	 */
	private static @NotNull Effects effects(@NotNull Instruction instruction, int registerCount) {
		BitSet uses = new BitSet(registerCount);
		BitSet definitions = new BitSet(registerCount);

		if (instruction instanceof ArrayInstruction array) {
			int opcode = array.opcode();
			switch (opcode) {
				case Opcodes.AGET,
				     Opcodes.AGET_WIDE,
				     Opcodes.AGET_OBJECT,
				     Opcodes.AGET_BOOLEAN,
				     Opcodes.AGET_BYTE,
				     Opcodes.AGET_CHAR,
				     Opcodes.AGET_SHORT -> {
					read(uses, array.array(), false, registerCount);
					read(uses, array.index(), false, registerCount);
					write(definitions, array.value(), opcode == Opcodes.AGET_WIDE, registerCount);
				}
				case Opcodes.APUT,
				     Opcodes.APUT_WIDE,
				     Opcodes.APUT_OBJECT,
				     Opcodes.APUT_BOOLEAN,
				     Opcodes.APUT_BYTE,
				     Opcodes.APUT_CHAR,
				     Opcodes.APUT_SHORT -> {
					read(uses, array.array(), false, registerCount);
					read(uses, array.index(), false, registerCount);
					read(uses, array.value(), opcode == Opcodes.APUT_WIDE, registerCount);
				}
				default -> readAll(uses, registerCount);
			}
		} else if (instruction instanceof ArrayLengthInstruction(int dest, int array)) {
			read(uses, array, false, registerCount);
			write(definitions, dest, false, registerCount);
		} else if (instruction instanceof Binary2AddrInstruction(int opcode, int a, int b)) {
			binaryEffects(uses, definitions, opcode, a, a, b, registerCount);
		} else if (instruction instanceof BinaryInstruction(int opcode, int dest, int a, int b)) {
			binaryEffects(uses, definitions, opcode, dest, a, b, registerCount);
		} else if (instruction instanceof BinaryLiteralInstruction binary) {
			read(uses, binary.src(), false, registerCount);
			write(definitions, binary.dest(), false, registerCount);
		} else if (instruction instanceof BranchInstruction branch) {
			read(uses, branch.a(), false, registerCount);
			read(uses, branch.b(), false, registerCount);
		} else if (instruction instanceof BranchZeroInstruction branch) {
			read(uses, branch.a(), false, registerCount);
		} else if (instruction instanceof CheckCastInstruction cast) {
			read(uses, cast.register(), false, registerCount);
			write(definitions, cast.register(), false, registerCount);
		} else if (instruction instanceof CompareInstruction(int opcode, int dest, int a, int b)) {
			boolean wide = opcode == Opcodes.CMP_LONG || opcode == Opcodes.CMPL_DOUBLE || opcode == Opcodes.CMPG_DOUBLE;
			read(uses, a, wide, registerCount);
			read(uses, b, wide, registerCount);
			write(definitions, dest, false, registerCount);
		} else if (instruction instanceof ConstInstruction constant) {
			write(definitions, constant.register(), false, registerCount);
		} else if (instruction instanceof ConstTypeInstruction constant) {
			write(definitions, constant.register(), false, registerCount);
		} else if (instruction instanceof ConstWideInstruction constant) {
			write(definitions, constant.register(), true, registerCount);
		} else if (instruction instanceof ConstStringInstruction constant) {
			write(definitions, constant.register(), false, registerCount);
		} else if (instruction instanceof ConstMethodHandleInstruction constant) {
			write(definitions, constant.destination(), false, registerCount);
		} else if (instruction instanceof ConstMethodTypeInstruction constant) {
			write(definitions, constant.destination(), false, registerCount);
		} else if (instruction instanceof FillArrayDataInstruction fillArrayData) {
			read(uses, fillArrayData.array(), false, registerCount);
		} else if (instruction instanceof FilledNewArrayInstruction filledArray) {
			if (filledArray.isRange())
				readRange(uses, filledArray.first(), filledArray.last(), registerCount);
			else if (filledArray.registers() != null)
				for (int register : filledArray.registers())
					read(uses, register, false, registerCount);
			else
				readAll(uses, registerCount);
		} else if (instruction instanceof GotoInstruction || instruction instanceof Label || instruction instanceof NopInstruction) {
			// These instructions do not access registers.
		} else if (instruction instanceof InstanceFieldInstruction field) {
			fieldEffects(uses, definitions, field.opcode(), field.value(), field.instance(), registerCount);
		} else if (instruction instanceof InstanceOfInstruction instanceOf) {
			read(uses, instanceOf.register(), false, registerCount);
			write(definitions, instanceOf.destination(), false, registerCount);
		} else if (instruction instanceof InvokeInstruction invoke) {
			if (invoke.isRange())
				readRange(uses, invoke.first(), invoke.last(), registerCount);
			else if (invoke.arguments() != null)
				for (int register : invoke.arguments())
					read(uses, register, false, registerCount);
			else
				readAll(uses, registerCount);
		} else if (instruction instanceof InvokeCustomInstruction invoke) {
			if (invoke.isRange())
				readRange(uses, invoke.first(), invoke.last(), registerCount);
			else if (invoke.argumentRegisters() != null)
				for (int register : invoke.argumentRegisters())
					read(uses, register, false, registerCount);
			else
				readAll(uses, registerCount);
		} else if (instruction instanceof MonitorInstruction monitor) {
			read(uses, monitor.register(), false, registerCount);
		} else if (instruction instanceof MoveExceptionInstruction(int register)) {
			write(definitions, register, false, registerCount);
		} else if (instruction instanceof MoveInstruction move) {
			read(uses, move.from(), false, registerCount);
			write(definitions, move.to(), false, registerCount);
		} else if (instruction instanceof MoveObjectInstruction move) {
			read(uses, move.from(), false, registerCount);
			write(definitions, move.to(), false, registerCount);
		} else if (instruction instanceof MoveResultInstruction moveResult) {
			write(definitions, moveResult.to(), moveResult.opcode() == Opcodes.MOVE_RESULT_WIDE, registerCount);
		} else if (instruction instanceof MoveWideInstruction move) {
			read(uses, move.from(), true, registerCount);
			write(definitions, move.to(), true, registerCount);
		} else if (instruction instanceof NewArrayInstruction array) {
			read(uses, array.sizeRegister(), false, registerCount);
			write(definitions, array.dest(), false, registerCount);
		} else if (instruction instanceof NewInstanceInstruction newInstance) {
			write(definitions, newInstance.dest(), false, registerCount);
		} else if (instruction instanceof PackedSwitchInstruction packedSwitch) {
			read(uses, packedSwitch.register(), false, registerCount);
		} else if (instruction instanceof ReturnInstruction returnInstruction) {
			if (returnInstruction.opcode() != Opcodes.RETURN_VOID)
				read(uses, returnInstruction.register(), returnInstruction.opcode() == Opcodes.RETURN_WIDE, registerCount);
		} else if (instruction instanceof SparseSwitchInstruction sparseSwitch) {
			read(uses, sparseSwitch.register(), false, registerCount);
		} else if (instruction instanceof StaticFieldInstruction field) {
			staticFieldEffects(uses, definitions, field.opcode(), field.value(), registerCount);
		} else if (instruction instanceof ThrowInstruction(int value)) {
			read(uses, value, false, registerCount);
		} else if (instruction instanceof UnaryInstruction(int opcode, int dest, int source)) {
			unaryEffects(uses, definitions, opcode, source, dest, registerCount);
		} else {
			// Dunno what this is, so assume it reads all registers and writes none.
			// This isn't great, but it should be a relatively safe fallback.
			readAll(uses, registerCount);
		}
		return new Effects(uses, definitions);
	}

	/**
	 * Adds effects for a register-register binary operation.
	 *
	 * @param uses
	 * 		Register words read by the instruction.
	 * @param definitions
	 * 		Register words written by the instruction.
	 * @param opcode
	 * 		Dalvik opcode determining the operand widths.
	 * @param destination
	 * 		Destination register.
	 * @param left
	 * 		Left source register.
	 * @param right
	 * 		Right source register.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void binaryEffects(@NotNull BitSet uses, @NotNull BitSet definitions, int opcode, int destination,
	                                  int left, int right, int registerCount) {
		boolean wide = isWideBinary(opcode);
		read(uses, left, wide, registerCount);
		read(uses, right, wide && !isLongShift(opcode), registerCount);
		write(definitions, destination, wide, registerCount);
	}

	/**
	 * @param opcode
	 * 		Dalvik binary opcode.
	 *
	 * @return {@code true} if the operation reads or writes a wide value.
	 */
	private static boolean isWideBinary(int opcode) {
		return switch (opcode) {
			case Opcodes.ADD_LONG,
			     Opcodes.SUB_LONG,
			     Opcodes.MUL_LONG,
			     Opcodes.DIV_LONG,
			     Opcodes.REM_LONG,
			     Opcodes.AND_LONG,
			     Opcodes.OR_LONG,
			     Opcodes.XOR_LONG,
			     Opcodes.SHL_LONG,
			     Opcodes.SHR_LONG,
			     Opcodes.USHR_LONG,
			     Opcodes.ADD_LONG_2ADDR,
			     Opcodes.SUB_LONG_2ADDR,
			     Opcodes.MUL_LONG_2ADDR,
			     Opcodes.DIV_LONG_2ADDR,
			     Opcodes.REM_LONG_2ADDR,
			     Opcodes.AND_LONG_2ADDR,
			     Opcodes.OR_LONG_2ADDR,
			     Opcodes.XOR_LONG_2ADDR,
			     Opcodes.SHL_LONG_2ADDR,
			     Opcodes.SHR_LONG_2ADDR,
			     Opcodes.USHR_LONG_2ADDR,
			     Opcodes.ADD_DOUBLE,
			     Opcodes.SUB_DOUBLE,
			     Opcodes.MUL_DOUBLE,
			     Opcodes.DIV_DOUBLE,
			     Opcodes.REM_DOUBLE,
			     Opcodes.ADD_DOUBLE_2ADDR,
			     Opcodes.SUB_DOUBLE_2ADDR,
			     Opcodes.MUL_DOUBLE_2ADDR,
			     Opcodes.DIV_DOUBLE_2ADDR,
			     Opcodes.REM_DOUBLE_2ADDR -> true;
			default -> false;
		};
	}

	/**
	 * @param opcode
	 * 		Dalvik binary opcode.
	 *
	 * @return {@code true} if the operation shifts a wide value by a single-word distance.
	 */
	private static boolean isLongShift(int opcode) {
		return switch (opcode) {
			case Opcodes.SHL_LONG,
			     Opcodes.SHR_LONG,
			     Opcodes.USHR_LONG,
			     Opcodes.SHL_LONG_2ADDR,
			     Opcodes.SHR_LONG_2ADDR,
			     Opcodes.USHR_LONG_2ADDR -> true;
			default -> false;
		};
	}

	/**
	 * Adds effects for a unary operation, whose input and output widths can differ during conversions.
	 *
	 * @param uses
	 * 		Register words read by the instruction.
	 * @param definitions
	 * 		Register words written by the instruction.
	 * @param opcode
	 * 		Dalvik opcode determining the input and output widths.
	 * @param source
	 * 		Source register.
	 * @param destination
	 * 		Destination register.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void unaryEffects(@NotNull BitSet uses, @NotNull BitSet definitions, int opcode, int source,
	                                 int destination, int registerCount) {
		boolean wideSource = switch (opcode) {
			case Opcodes.NEG_LONG,
			     Opcodes.NOT_LONG,
			     Opcodes.NEG_DOUBLE,
			     Opcodes.LONG_TO_INT,
			     Opcodes.LONG_TO_FLOAT,
			     Opcodes.LONG_TO_DOUBLE,
			     Opcodes.DOUBLE_TO_INT,
			     Opcodes.DOUBLE_TO_LONG,
			     Opcodes.DOUBLE_TO_FLOAT -> true;
			default -> false;
		};
		boolean wideDestination = switch (opcode) {
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
		read(uses, source, wideSource, registerCount);
		write(definitions, destination, wideDestination, registerCount);
	}

	/**
	 * Adds effects for an instance-field get or put.
	 *
	 * @param uses
	 * 		Register words read by the instruction.
	 * @param definitions
	 * 		Register words written by the instruction.
	 * @param opcode
	 * 		Field opcode determining whether {@code value} is read or written and its width.
	 * @param value
	 * 		Register holding the field value.
	 * @param instance
	 * 		Register holding the object instance.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void fieldEffects(@NotNull BitSet uses, @NotNull BitSet definitions, int opcode, int value,
	                                 int instance, int registerCount) {
		boolean wide = opcode == Opcodes.IGET_WIDE || opcode == Opcodes.IPUT_WIDE;
		read(uses, instance, false, registerCount);
		switch (opcode) {
			case Opcodes.IGET,
			     Opcodes.IGET_WIDE,
			     Opcodes.IGET_OBJECT,
			     Opcodes.IGET_BOOLEAN,
			     Opcodes.IGET_BYTE,
			     Opcodes.IGET_CHAR,
			     Opcodes.IGET_SHORT -> write(definitions, value, wide, registerCount);
			case Opcodes.IPUT,
			     Opcodes.IPUT_WIDE,
			     Opcodes.IPUT_OBJECT,
			     Opcodes.IPUT_BOOLEAN,
			     Opcodes.IPUT_BYTE,
			     Opcodes.IPUT_CHAR,
			     Opcodes.IPUT_SHORT -> read(uses, value, wide, registerCount);
			default -> readAll(uses, registerCount);
		}
	}

	/**
	 * Adds effects for a static-field get or put.
	 *
	 * @param uses
	 * 		Register words read by the instruction.
	 * @param definitions
	 * 		Register words written by the instruction.
	 * @param opcode
	 * 		Field opcode determining whether {@code value} is read or written and its width.
	 * @param value
	 * 		Register holding the field value.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void staticFieldEffects(@NotNull BitSet uses, @NotNull BitSet definitions, int opcode, int value,
	                                       int registerCount) {
		boolean wide = opcode == Opcodes.SGET_WIDE || opcode == Opcodes.SPUT_WIDE;
		switch (opcode) {
			case Opcodes.SGET,
			     Opcodes.SGET_WIDE,
			     Opcodes.SGET_OBJECT,
			     Opcodes.SGET_BOOLEAN,
			     Opcodes.SGET_BYTE,
			     Opcodes.SGET_CHAR,
			     Opcodes.SGET_SHORT -> write(definitions, value, wide, registerCount);
			case Opcodes.SPUT,
			     Opcodes.SPUT_WIDE,
			     Opcodes.SPUT_OBJECT,
			     Opcodes.SPUT_BOOLEAN,
			     Opcodes.SPUT_BYTE,
			     Opcodes.SPUT_CHAR,
			     Opcodes.SPUT_SHORT -> read(uses, value, wide, registerCount);
			default -> readAll(uses, registerCount);
		}
	}

	/**
	 * Marks every register in an inclusive range as read.
	 *
	 * @param uses
	 * 		Register words read by the instruction.
	 * @param first
	 * 		First register in the range.
	 * @param last
	 * 		Last register in the range.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void readRange(@NotNull BitSet uses, int first, int last, int registerCount) {
		if (first > last || first < 0 || last >= registerCount) {
			// Invalid range, so conservatively mark every register live.
			readAll(uses, registerCount);
			return;
		}
		uses.set(first, last + 1);
	}

	/**
	 * Marks one register, and its adjacent tail when wide, as read.
	 * <p>
	 * An invalid register index is handled conservatively by marking every register live.
	 *
	 * @param uses
	 * 		Register words read by the instruction.
	 * @param register
	 * 		Register index holding the value.
	 * @param wide
	 * 		Whether the value occupies two register words.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void read(@NotNull BitSet uses, int register, boolean wide, int registerCount) {
		if (register < 0 || register >= registerCount) {
			// Invalid register, so conservatively mark every register live.
			readAll(uses, registerCount);
			return;
		}
		uses.set(register);
		if (wide && register + 1 < registerCount)
			uses.set(register + 1);
	}

	/**
	 * Marks one register, and its adjacent tail when wide, as defined.
	 *
	 * @param definitions
	 * 		Register words written by the instruction.
	 * @param register
	 * 		Destination register index.
	 * @param wide
	 * 		Whether the value occupies two register words.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void write(@NotNull BitSet definitions, int register, boolean wide, int registerCount) {
		// Invalid destination is ignored because it cannot kill a valid register's liveness.
		if (register < 0 || register >= registerCount)
			return;

		definitions.set(register);
		if (wide && register + 1 < registerCount)
			definitions.set(register + 1);
	}

	/**
	 * Marks every register as read when an instruction's effects are unknown or malformed.
	 *
	 * @param uses
	 * 		Register words read by the instruction.
	 * @param registerCount
	 * 		Number of register words in the method.
	 */
	private static void readAll(@NotNull BitSet uses, int registerCount) {
		uses.set(0, registerCount);
	}

	/**
	 * Lazily computes and caches the live-in sets for one method.
	 */
	static final class Lookup {
		private final @NotNull Code code;
		private final @NotNull DalvikControlFlowGraph graph;
		private @Nullable BitSet[] liveIn;

		/**
		 * @param code
		 * 		Method code whose register accesses are analyzed.
		 * @param graph
		 * 		Control-flow graph for {@code code}.
		 */
		private Lookup(@NotNull Code code, @NotNull DalvikControlFlowGraph graph) {
			this.code = code;
			this.graph = graph;
		}

		/**
		 * Checks whether a register may be read before being overwritten at an instruction.
		 *
		 * @param instructionIndex
		 * 		Instruction index whose live-in set is checked.
		 * @param register
		 * 		Register word to check.
		 *
		 * @return {@code true} when the register is live before the instruction.
		 */
		boolean contains(int instructionIndex, int register) {
			BitSet[] currentLiveIn = liveIn;
			if (currentLiveIn == null) {
				currentLiveIn = computeLiveIn(code, graph);
				liveIn = currentLiveIn;
			}
			return instructionIndex >= 0 && instructionIndex < currentLiveIn.length
					&& currentLiveIn[instructionIndex].get(register);
		}
	}

	/**
	 * Register-word reads and writes for one instruction.
	 *
	 * @param uses
	 * 		Words read by the instruction.
	 * @param definitions
	 * 		Words written by the instruction.
	 */
	private record Effects(@NotNull BitSet uses, @NotNull BitSet definitions) {}
}
