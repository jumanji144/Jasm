package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.AnalysisWorklist;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
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
import me.darknet.dex.tree.simulation.ExecutionEngine;
import me.darknet.dex.tree.simulation.StraightForwardSimulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds normal and exceptional Dalvik CFG edges.
 */
final class DalvikControlFlowGraphBuilder {
	private static final String UNRESOLVABLE_CONTROL_TARGET = "Unresolvable control-flow target label";
	private static final String UNRESOLVABLE_EXCEPTION_TARGET = "Unresolvable exception range or handler label";
	private static final String UNSUPPORTED_INSTRUCTION = "Unsupported instruction during Dalvik CFG construction";

	private final Code code;
	private final List<Instruction> instructions;
	private final Map<Instruction, ASTInstruction> instructionToSource;
	private final Map<Instruction, Integer> instructionIndices = new IdentityHashMap<>();
	private final Map<Label, Integer> labelIndices = new IdentityHashMap<>();
	private final Map<Integer, List<Integer>> normalSuccessors = new LinkedHashMap<>();
	private final Map<Integer, List<DalvikControlFlowGraph.ExceptionEdge>> exceptionalSuccessors = new LinkedHashMap<>();
	private final List<DalvikAnalysisFailure> failures = new ArrayList<>();
	private Integer entryIndex;

	private DalvikControlFlowGraphBuilder(Code code, Map<Instruction, ASTInstruction> instructionToSource) {
		this.code = code;
		this.instructions = code.getInstructions();
		this.instructionToSource = instructionToSource;
		indexInstructions();
	}

	static DalvikControlFlowGraph build(Code code, Map<Instruction, ASTInstruction> instructionToSource) {
		return new DalvikControlFlowGraphBuilder(code, instructionToSource).build();
	}

	private DalvikControlFlowGraph build() {
		new StraightForwardSimulation().execute(new FlowEngine(), code);
		buildExceptionalEdges();
		return new DalvikControlFlowGraph(entryIndex, normalSuccessors, exceptionalSuccessors,
				computeReachableIndices(), failures);
	}

	private void indexInstructions() {
		for (int index = 0; index < instructions.size(); index++) {
			Instruction instruction = instructions.get(index);
			instructionIndices.put(instruction, index);
			if (instruction instanceof Label label) {
				labelIndices.put(label, index);
			} else {
				normalSuccessors.put(index, new ArrayList<>());
				exceptionalSuccessors.put(index, new ArrayList<>());
				if (entryIndex == null)
					entryIndex = index;
			}
		}
	}

	private List<Integer> fallthrough(int instructionIndex) {
		Integer next = nextExecutableIndex(instructionIndex + 1);
		return next == null ? List.of() : List.of(next);
	}

	private Integer nextExecutableIndex(int startIndex) {
		for (int index = startIndex; index < instructions.size(); index++)
			if (!(instructions.get(index) instanceof Label))
				return index;
		return null;
	}

	private Integer resolveTarget(Label label) {
		Integer labelIndex = labelIndices.get(label);
		return labelIndex == null ? null : nextExecutableIndex(labelIndex);
	}

	private void addTarget(List<Integer> successors, Label label, int sourceIndex) {
		Integer targetIndex = resolveTarget(label);
		if (targetIndex == null) {
			addFailure(sourceIndex, UNRESOLVABLE_CONTROL_TARGET);
			return;
		}
		if (!successors.contains(targetIndex))
			successors.add(targetIndex);
	}

	private void setSuccessors(int index, List<Integer> successors) {
		normalSuccessors.put(index, successors);
	}

	private void addFailure(Integer instructionIndex, String message) {
		ASTInstruction source = instructionIndex == null
				? null
				: instructionToSource.get(instructions.get(instructionIndex));
		failures.add(new DalvikAnalysisFailure(DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE,
				instructionIndex, message, source == null ? null : source.location()));
	}

	private void buildExceptionalEdges() {
		List<TryCatch> tryCatches = code.tryCatch();
		for (int tryCatchOrder = 0; tryCatchOrder < tryCatches.size(); tryCatchOrder++) {
			TryCatch tryCatch = tryCatches.get(tryCatchOrder);
			Integer beginIndex = labelIndices.get(tryCatch.begin());
			Integer endIndex = labelIndices.get(tryCatch.end());
			if (beginIndex == null || endIndex == null || endIndex <= beginIndex || tryCatch.handlers().isEmpty()) {
				addFailure(beginIndex == null ? endIndex : beginIndex, UNRESOLVABLE_EXCEPTION_TARGET);
				continue;
			}

			Integer rangeOrigin = firstExecutableIndex(beginIndex, endIndex);
			List<ResolvedHandler> handlers = new ArrayList<>(tryCatch.handlers().size());
			for (int handlerOrder = 0; handlerOrder < tryCatch.handlers().size(); handlerOrder++) {
				Handler handler = tryCatch.handlers().get(handlerOrder);
				Integer targetIndex = resolveTarget(handler.handler());
				if (targetIndex == null) {
					addFailure(rangeOrigin, UNRESOLVABLE_EXCEPTION_TARGET);
					continue;
				}
				ClassDescriptor exceptionType = handler.exceptionType() == null
						? null
						: new ClassDescriptor(handler.exceptionType().internalName());
				handlers.add(new ResolvedHandler(targetIndex, exceptionType, handlerOrder, handler.handler()));
			}

			for (int instructionIndex = beginIndex; instructionIndex < endIndex; instructionIndex++) {
				Instruction instruction = instructions.get(instructionIndex);
				if (instruction instanceof Label || !mayThrow(instruction))
					continue;
				List<DalvikControlFlowGraph.ExceptionEdge> edges = exceptionalSuccessors.get(instructionIndex);
				for (ResolvedHandler handler : handlers)
					edges.add(new DalvikControlFlowGraph.ExceptionEdge(handler.targetIndex(), handler.exceptionType(),
							tryCatchOrder, handler.handlerOrder(), handler.handlerLabel()));
			}
		}
	}

	private Integer firstExecutableIndex(int beginIndex, int endIndex) {
		for (int index = beginIndex; index < endIndex; index++)
			if (!(instructions.get(index) instanceof Label))
				return index;
		return null;
	}

	private Set<Integer> computeReachableIndices() {
		if (entryIndex == null)
			return Set.of();

		AnalysisWorklist worklist = new AnalysisWorklist();
		Set<Integer> reachable = new LinkedHashSet<>();
		worklist.add(entryIndex);
		AnalysisWorklist.Entry entry;
		while ((entry = worklist.next()) != null) {
			int index = entry.index();
			if (!reachable.add(index))
				continue;
			for (int successor : normalSuccessors.getOrDefault(index, List.of()))
				if (!reachable.contains(successor))
					worklist.add(successor);
			for (DalvikControlFlowGraph.ExceptionEdge edge : exceptionalSuccessors.getOrDefault(index, List.of()))
				if (!reachable.contains(edge.targetIndex()))
					worklist.add(edge.targetIndex());
		}
		return reachable;
	}

	private static boolean mayThrow(Instruction instruction) {
		if (instruction instanceof ThrowInstruction
				|| instruction instanceof ArrayInstruction
				|| instruction instanceof ArrayLengthInstruction
				|| instruction instanceof FillArrayDataInstruction
				|| instruction instanceof CheckCastInstruction
				|| instruction instanceof InstanceOfInstruction
				|| instruction instanceof MonitorInstruction
				|| instruction instanceof NewArrayInstruction
				|| instruction instanceof NewInstanceInstruction
				|| instruction instanceof FilledNewArrayInstruction
				|| instruction instanceof InstanceFieldInstruction
				|| instruction instanceof StaticFieldInstruction
				|| instruction instanceof InvokeInstruction
				|| instruction instanceof InvokeCustomInstruction
				|| instruction instanceof ConstStringInstruction
				|| instruction instanceof ConstTypeInstruction
				|| instruction instanceof ConstMethodHandleInstruction
				|| instruction instanceof ConstMethodTypeInstruction)
			return true;

		int opcode = instruction.opcode();
		return (instruction instanceof BinaryInstruction
				|| instruction instanceof Binary2AddrInstruction
				|| instruction instanceof BinaryLiteralInstruction)
				&& switch (opcode) {
			case Opcodes.DIV_INT, Opcodes.REM_INT, Opcodes.DIV_LONG, Opcodes.REM_LONG,
			     Opcodes.DIV_INT_2ADDR, Opcodes.REM_INT_2ADDR, Opcodes.DIV_LONG_2ADDR, Opcodes.REM_LONG_2ADDR,
			     Opcodes.DIV_INT_LIT16, Opcodes.REM_INT_LIT16, Opcodes.DIV_INT_LIT8, Opcodes.REM_INT_LIT8 -> true;
			default -> false;
		};
	}

	private record ResolvedHandler(int targetIndex, @Nullable ClassDescriptor exceptionType,
	                               int handlerOrder, Label handlerLabel) {}

	private final class FlowEngine implements ExecutionEngine {
		private void ordinary(Instruction instruction) {
			Integer index = instructionIndices.get(instruction);
			if (index == null) {
				addFailure(null, UNSUPPORTED_INSTRUCTION);
				return;
			}
			setSuccessors(index, fallthrough(index));
		}

		private void branch(Instruction instruction, Label target, boolean conditional) {
			Integer index = instructionIndices.get(instruction);
			if (index == null) {
				addFailure(null, UNSUPPORTED_INSTRUCTION);
				return;
			}
			List<Integer> successors = new ArrayList<>(conditional ? 2 : 1);
			addTarget(successors, target, index);
			if (conditional) {
				for (int successor : fallthrough(index))
					if (!successors.contains(successor))
						successors.add(successor);
			}
			setSuccessors(index, successors);
		}

		private void switchTargets(Instruction instruction, Iterable<Label> targets) {
			Integer index = instructionIndices.get(instruction);
			if (index == null) {
				addFailure(null, UNSUPPORTED_INSTRUCTION);
				return;
			}
			List<Integer> successors = new ArrayList<>();
			for (Label target : targets)
				addTarget(successors, target, index);
			for (int successor : fallthrough(index))
				if (!successors.contains(successor))
					successors.add(successor);
			setSuccessors(index, successors);
		}

		@Override
		public void label(@NotNull Label label) {}

		@Override
		public void execute(@NotNull ArrayInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ArrayLengthInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull Binary2AddrInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull BinaryInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull BinaryLiteralInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull BranchInstruction instruction) {
			branch(instruction, instruction.label(), true);
		}

		@Override
		public void execute(@NotNull BranchZeroInstruction instruction) {
			branch(instruction, instruction.label(), true);
		}

		@Override
		public void execute(@NotNull CheckCastInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull CompareInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ConstInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ConstTypeInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ConstWideInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ConstStringInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ConstMethodHandleInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ConstMethodTypeInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull FillArrayDataInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull FilledNewArrayInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull GotoInstruction instruction) {
			branch(instruction, instruction.jump(), false);}

		@Override
		public void execute(@NotNull InstanceFieldInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull InstanceOfInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull InvokeCustomInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull InvokeInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull MonitorInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull MoveExceptionInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull MoveInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull MoveObjectInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull MoveResultInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull MoveWideInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull NewArrayInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull NewInstanceInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull NopInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull PackedSwitchInstruction instruction) {
			switchTargets(instruction, instruction.targets());
		}

		@Override
		public void execute(@NotNull ReturnInstruction instruction) {
			Integer index = instructionIndices.get(instruction);
			if (index != null)
				setSuccessors(index, List.of());
			else
				addFailure(null, UNSUPPORTED_INSTRUCTION);
		}

		@Override
		public void execute(@NotNull SparseSwitchInstruction instruction) {
			switchTargets(instruction, new TreeMap<>(instruction.targets()).values());
		}

		@Override
		public void execute(@NotNull StaticFieldInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull ThrowInstruction instruction) {
			Integer index = instructionIndices.get(instruction);
			if (index != null)
				setSuccessors(index, List.of());
			else
				addFailure(null, UNSUPPORTED_INSTRUCTION);
		}

		@Override
		public void execute(@NotNull UnaryInstruction instruction) {
			ordinary(instruction);
		}

		@Override
		public void execute(@NotNull Instruction instruction) {
			if (DalvikInstructionDispatcher.dispatch(instruction, this))
				return;
			Integer index = instructionIndices.get(instruction);
			addFailure(index, UNSUPPORTED_INSTRUCTION);
			if (index != null)
				setSuccessors(index, List.of());
		}
	}
}
