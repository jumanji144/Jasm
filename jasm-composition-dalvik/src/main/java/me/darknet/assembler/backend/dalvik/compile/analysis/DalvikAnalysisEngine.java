package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.AnalysisWorklist;
import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.analysis.registry.FieldValueLookup;
import me.darknet.assembler.analysis.registry.MethodValueLookup;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.backend.dalvik.util.DalvikTypeUtils;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorParser;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.type.ClassType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DalvikAnalysisEngine {
	private static final String REGISTER_LAYOUT_FAILURE = "Method register layout does not match descriptor";

	private DalvikAnalysisEngine() {}

	/**
	 * Analyzes emitted method code and seeds its entry register state.
	 *
	 * @param method
	 * 		Full method reference.
	 * @param member
	 * 		Emitted method metadata and descriptor.
	 * @param code
	 * 		Completed emitted code.
	 * @param instructionToSource
	 * 		Identity mapping from emitted instructions to source instructions.
	 *
	 * @return Analysis results for the given method.
	 */
	public static @NotNull DalvikAnalysisResults analyze(@NotNull MethodReference method,
	                                                     @NotNull MethodMember member,
	                                                     @NotNull Code code,
	                                                     @NotNull Map<Instruction, ASTInstruction> instructionToSource,
	                                                     @NotNull InheritanceChecker inheritanceChecker,
	                                                     @NotNull MethodValueLookup methodValueLookup,
	                                                     @NotNull FieldValueLookup fieldValueLookup) {
		DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(code, instructionToSource);
		List<DalvikAnalysisFailure> failures = new ArrayList<>(graph.failures());
		Map<Integer, DalvikRegisterState> instructionStates = new TreeMap<>();
		Map<Integer, DalvikAnalysisFrame> inputFrames = new TreeMap<>();
		Map<Integer, DalvikAnalysisResults.TerminalState> terminalStates = new TreeMap<>();

		if (graph.entryIndex() != null && failures.isEmpty()) {
			DalvikRegisterState entryState = seedEntryState(method, member, code, graph.entryIndex(), instructionToSource, failures);
			if (entryState != null) {
				inputFrames.put(graph.entryIndex(), new DalvikAnalysisFrame(entryState));
				AnalysisWorklist worklist = new AnalysisWorklist();
				Set<Integer> queuedIndices = new HashSet<>();
				worklist.add(graph.entryIndex());
				queuedIndices.add(graph.entryIndex());

				Set<FailureKey> failureKeys = new HashSet<>();
				for (DalvikAnalysisFailure failure : failures)
					failureKeys.add(new FailureKey(failure.instructionIndex(), failure.kind(), failure.message()));

				DalvikInstructionTransferEngine transferEngine = new DalvikInstructionTransferEngine(
						inheritanceChecker, methodValueLookup, fieldValueLookup, instructionToSource,
						(index, kind, message) -> recordFailure(index, kind, message, code, instructionToSource, failures, failureKeys)
				);

				AnalysisWorklist.Entry work;
				while ((work = worklist.next()) != null) {
					int index = work.index();
					queuedIndices.remove(index);
					DalvikAnalysisFrame input = inputFrames.get(index);
					if (input == null || index < 0 || index >= code.getInstructions().size())
						continue;

					Instruction instruction = code.getInstructions().get(index);
					DalvikInstructionTransferEngine.TransferResult transfer = transferEngine.transfer(index, instruction, input);
					if (transfer.terminalState() == null)
						terminalStates.remove(index);
					else
						terminalStates.put(index, transfer.terminalState());

					for (int successor : graph.normalSuccessors().getOrDefault(index, List.of()))
						propagate(successor, transfer.output(), inputFrames, inheritanceChecker, worklist, queuedIndices,
								code, instructionToSource, failures, failureKeys);
					for (DalvikControlFlowGraph.ExceptionEdge edge : graph.exceptionalSuccessors().getOrDefault(index, List.of())) {
						DalvikAnalysisFrame exceptionalInput = input.copy();
						exceptionalInput.clearPendingResult();
						ClassDescriptor exceptionType = edge.exceptionType() == null
								? new ClassDescriptor("java/lang/Throwable")
								: edge.exceptionType();
						exceptionalInput.setPendingException(Values.valueOfInstance(exceptionType));
						propagate(edge.targetIndex(), exceptionalInput, inputFrames, inheritanceChecker, worklist, queuedIndices,
								code, instructionToSource, failures, failureKeys);
					}
				}
			}
		}

		inputFrames.forEach((index, frame) -> instructionStates.put(index, frame.snapshot()));
		return new DalvikAnalysisResults(method, code.getInstructions(), instructionStates, terminalStates, instructionToSource, failures);
	}

	private static void propagate(int targetIndex,
	                              @NotNull DalvikAnalysisFrame incoming,
	                              @NotNull Map<Integer, DalvikAnalysisFrame> inputFrames,
	                              @NotNull InheritanceChecker inheritanceChecker,
	                              @NotNull AnalysisWorklist worklist,
	                              @NotNull Set<Integer> queuedIndices,
	                              @NotNull Code code,
	                              Map<Instruction, ASTInstruction> instructionToSource,
	                              List<DalvikAnalysisFailure> failures,
	                              Set<FailureKey> failureKeys) {
		DalvikAnalysisFrame target = inputFrames.get(targetIndex);
		boolean changed;
		if (target == null) {
			inputFrames.put(targetIndex, incoming.copy());
			changed = true;
		} else {
			DalvikRegisterStateMerger.MergeResult result = DalvikRegisterStateMerger.merge(inheritanceChecker, target, incoming);
			changed = result.changed();
			if (!result.incompatibleRegisters().isEmpty()) {
				Set<Integer> orderedRegisters = new TreeSet<>(result.incompatibleRegisters());
				recordFailure(targetIndex, DalvikAnalysisFailure.FailureKind.INCOMPATIBLE_MERGE,
						"Incompatible register merge at " + orderedRegisters, code, instructionToSource, failures, failureKeys);
			}
		}
		if (changed && queuedIndices.add(targetIndex))
			worklist.add(targetIndex);
	}

	private static void recordFailure(int instructionIndex,
	                                  DalvikAnalysisFailure.FailureKind kind,
	                                  String message,
	                                  Code code,
	                                  Map<Instruction, ASTInstruction> instructionToSource,
	                                  List<DalvikAnalysisFailure> failures,
	                                  Set<FailureKey> failureKeys) {
		FailureKey key = new FailureKey(instructionIndex, kind, message);
		if (!failureKeys.add(key))
			return;

		ASTInstruction source = instructionIndex >= 0
				&& instructionIndex < code.getInstructions().size()
				? instructionToSource.get(code.getInstructions().get(instructionIndex)) : null;
		failures.add(new DalvikAnalysisFailure(kind, instructionIndex, message, source == null ? null : source.location()));
	}

	private record FailureKey(Integer instructionIndex, DalvikAnalysisFailure.FailureKind kind, String message) {}

	private static @Nullable DalvikRegisterState seedEntryState(@NotNull MethodReference method,
	                                                            @NotNull MethodMember member,
	                                                            @NotNull Code code,
	                                                            int entryIndex,
	                                                            @NotNull Map<Instruction, ASTInstruction> instructionToSource,
	                                                            @NotNull List<DalvikAnalysisFailure> failures) {
		boolean isStatic = (member.getAccess() & DalvikModifiers.ACC_STATIC) != 0;
		int registerCount = code.getRegisters();
		int incomingWordCount = isStatic ? 0 : 1;
		List<DescriptorType> parameterTypes = new ArrayList<>(member.getType().parameterTypes().size());
		for (ClassType parameter : member.getType().parameterTypes()) {
			DescriptorType type = DescriptorParser.parseFieldDescriptor(parameter.descriptor());
			parameterTypes.add(type);
			incomingWordCount += DalvikTypeUtils.isWideType(type) ? 2 : 1;
		}

		if (registerCount < incomingWordCount || incomingWordCount != code.getIn()) {
			ASTInstruction source = instructionToSource.get(code.getInstructions().get(entryIndex));
			failures.add(new DalvikAnalysisFailure(DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE,
					entryIndex, REGISTER_LAYOUT_FAILURE, source == null ? null : source.location()));
			return null;
		}

		int parameterBase = registerCount - incomingWordCount;
		List<DalvikRegisterState.Slot> slots = new ArrayList<>(registerCount);
		for (int register = 0; register < registerCount; register++)
			slots.add(DalvikRegisterState.Undefined.INSTANCE);

		int register = parameterBase;
		if (!isStatic) {
			ClassDescriptor owner = new ClassDescriptor(method.owner());
			Value receiver = method.name().equals("<init>")
					? new Value.UninitializedReferenceValue(owner, -1)
					: Values.valueOfInstance(owner);
			slots.set(register++, new DalvikRegisterState.ValueHead(receiver));
		}

		for (DescriptorType parameterType : parameterTypes) {
			Value value = Values.valueOf(parameterType);
			slots.set(register, new DalvikRegisterState.ValueHead(value));
			if (DalvikTypeUtils.isWideType(parameterType))
				slots.set(register + 1, new DalvikRegisterState.WideTail(register));
			register += DalvikTypeUtils.isWideType(parameterType) ? 2 : 1;
		}
		return new DalvikRegisterState(slots, null);
	}
}
