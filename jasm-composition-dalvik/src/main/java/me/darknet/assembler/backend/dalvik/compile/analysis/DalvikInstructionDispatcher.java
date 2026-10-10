package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.simulation.ExecutionEngine;
import org.jetbrains.annotations.NotNull;

/**
 * Dispatches Dalvik instructions to the appropriate methods in the {@link ExecutionEngine}.
 */
final class DalvikInstructionDispatcher {
	private DalvikInstructionDispatcher() {}

	static boolean dispatch(@NotNull Instruction instruction, @NotNull ExecutionEngine engine) {
		switch (instruction) {
			case Label label -> engine.label(label);
			case ArrayInstruction value -> engine.execute(value);
			case ArrayLengthInstruction value -> engine.execute(value);
			case Binary2AddrInstruction value -> engine.execute(value);
			case BinaryInstruction value -> engine.execute(value);
			case BinaryLiteralInstruction value -> engine.execute(value);
			case BranchInstruction value -> engine.execute(value);
			case BranchZeroInstruction value -> engine.execute(value);
			case CheckCastInstruction value -> engine.execute(value);
			case CompareInstruction value -> engine.execute(value);
			case ConstInstruction value -> engine.execute(value);
			case ConstTypeInstruction value -> engine.execute(value);
			case ConstWideInstruction value -> engine.execute(value);
			case ConstStringInstruction value -> engine.execute(value);
			case ConstMethodHandleInstruction value -> engine.execute(value);
			case ConstMethodTypeInstruction value -> engine.execute(value);
			case FillArrayDataInstruction value -> engine.execute(value);
			case FilledNewArrayInstruction value -> engine.execute(value);
			case GotoInstruction value -> engine.execute(value);
			case InstanceFieldInstruction value -> engine.execute(value);
			case InstanceOfInstruction value -> engine.execute(value);
			case InvokeCustomInstruction value -> engine.execute(value);
			case InvokeInstruction value -> engine.execute(value);
			case MonitorInstruction value -> engine.execute(value);
			case MoveExceptionInstruction value -> engine.execute(value);
			case MoveInstruction value -> engine.execute(value);
			case MoveObjectInstruction value -> engine.execute(value);
			case MoveResultInstruction value -> engine.execute(value);
			case MoveWideInstruction value -> engine.execute(value);
			case NewArrayInstruction value -> engine.execute(value);
			case NewInstanceInstruction value -> engine.execute(value);
			case NopInstruction value -> engine.execute(value);
			case PackedSwitchInstruction value -> engine.execute(value);
			case ReturnInstruction value -> engine.execute(value);
			case SparseSwitchInstruction value -> engine.execute(value);
			case StaticFieldInstruction value -> engine.execute(value);
			case ThrowInstruction value -> engine.execute(value);
			case UnaryInstruction value -> engine.execute(value);
			default -> {
				return false;
			}
		}
		return true;
	}
}
