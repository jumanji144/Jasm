package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.AnalysisWorklist;
import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.analysis.registry.FieldValueLookup;
import me.darknet.assembler.analysis.registry.MethodValueLookup;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorParser;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.Descriptors;
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

/**
 * Engine for analyzing Dalvik method code and populating register states.
 */
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
	 * @param inheritanceChecker
	 * 		Checker used to merge types along converging control flow paths.
	 * @param methodValueLookup
	 * 		Lookup for the values produced by invoked methods.
	 * @param fieldValueLookup
	 * 		Lookup for the values read from fields.
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
		List<Instruction> instructions = code.getInstructions();
		DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(code, instructionToSource);
		DalvikRegisterLiveness.Lookup liveRegisters = DalvikRegisterLiveness.lookup(code, graph);
		FailureCollector failureCollector = new FailureCollector(instructions, instructionToSource, graph.failures());
		Map<Integer, DalvikAnalysisFrame> inputFrames = new TreeMap<>();
		Map<Integer, DalvikAnalysisResults.TerminalState> terminalStates = new TreeMap<>();

		// Any graph without an entry instruction is unreachable, so no analysis is performed.
		Integer entryIndex = graph.entryIndex();
		if (entryIndex != null && graph.failures().isEmpty()) {
			DalvikRegisterState entryState = seedEntryState(method, member, code, entryIndex, failureCollector);
			if (entryState != null) {
				// Seed the entry frame.
				inputFrames.put(entryIndex, new DalvikAnalysisFrame(entryState));
				AnalysisWorklist worklist = new AnalysisWorklist();
				Set<Integer> queuedIndices = new HashSet<>();
				worklist.add(entryIndex);
				queuedIndices.add(entryIndex);

				// Set up the engine that transfers input frames through instructions to produce output frames.
				DalvikInstructionTransferEngine transferEngine = new DalvikInstructionTransferEngine(inheritanceChecker, methodValueLookup, fieldValueLookup, instructionToSource, failureCollector::record);

				// Process the worklist until no more instructions are queued.
				AnalysisWorklist.Entry work;
				while ((work = worklist.next()) != null) {
					int index = work.index();
					queuedIndices.remove(index);

					// Skip unreachable instructions.
					DalvikAnalysisFrame input = inputFrames.get(index);
					if (input == null || index < 0 || index >= instructions.size())
						continue;

					// Transfer the input frame through the instruction to produce an output frame and a terminal state.
					Instruction instruction = instructions.get(index);
					DalvikInstructionTransferEngine.TransferResult transfer = transferEngine.transfer(index, instruction, input);
					if (transfer.terminalState() == null)
						terminalStates.remove(index);
					else
						terminalStates.put(index, transfer.terminalState());

					// Propagate the frame the instruction produced along every normal edge leaving it.
					for (int successor : graph.normalSuccessors().getOrDefault(index, List.of()))
						propagate(successor, transfer.output(), inputFrames, liveRegisters, inheritanceChecker, worklist, queuedIndices, failureCollector);

					// Propagate to every exception handler that can catch an exception thrown by the instruction.
					for (DalvikControlFlowGraph.ExceptionEdge edge : graph.exceptionalSuccessors().getOrDefault(index, List.of())) {
						DalvikAnalysisFrame exceptionalInput = input.copy();
						exceptionalInput.clearPendingResult();
						ClassDescriptor exceptionType = edge.exceptionType() == null
								? new ClassDescriptor("java/lang/Throwable")
								: edge.exceptionType();
						exceptionalInput.setPendingException(Values.valueOfInstance(exceptionType));
						propagate(edge.targetIndex(), exceptionalInput, inputFrames, liveRegisters, inheritanceChecker, worklist, queuedIndices, failureCollector);
					}
				}
			}
		}

		Map<Integer, DalvikRegisterState> instructionStates = new TreeMap<>();
		inputFrames.forEach((index, frame) -> instructionStates.put(index, frame.snapshot()));
		return new DalvikAnalysisResults(method, instructions, instructionStates, terminalStates, instructionToSource, failureCollector.failures());
	}

	/**
	 * Propagates {@code incoming} to the input frame of {@code targetIndex}.
	 *
	 * @param targetIndex
	 * 		Instruction index to propagate to.
	 * @param incoming
	 * 		Frame to propagate.
	 * @param inputFrames
	 * 		Input frames by instruction index.
	 * @param liveRegisters
	 * 		Lookup for registers live at each merge target.
	 * @param inheritanceChecker
	 * 		Checker used to merge reference types.
	 * @param worklist
	 * 		Worklist of instructions to process.
	 * @param queuedIndices
	 * 		Indices currently in the worklist.
	 * @param failureCollector
	 * 		Collector for reported failures.
	 */
	private static void propagate(int targetIndex,
	                              @NotNull DalvikAnalysisFrame incoming,
	                              @NotNull Map<Integer, DalvikAnalysisFrame> inputFrames,
	                              @NotNull DalvikRegisterLiveness.Lookup liveRegisters,
	                              @NotNull InheritanceChecker inheritanceChecker,
	                              @NotNull AnalysisWorklist worklist,
	                              @NotNull Set<Integer> queuedIndices,
	                              @NotNull FailureCollector failureCollector) {
		DalvikAnalysisFrame target = inputFrames.get(targetIndex);
		boolean changed;
		if (target == null) {
			inputFrames.put(targetIndex, incoming.copy());
			changed = true;
		} else {
			DalvikRegisterStateMerger.MergeResult result = DalvikRegisterStateMerger.merge(inheritanceChecker, target, incoming);
			changed = result.changed();
			if (!result.incompatibleRegisters().isEmpty()) {
				// A conflicting value is only observable when some successor can read the register before replacing it.
				Set<Integer> orderedRegisters = new TreeSet<>();
				for (int register : result.incompatibleRegisters())
					if (liveRegisters.contains(targetIndex, register))
						orderedRegisters.add(register);
				if (!orderedRegisters.isEmpty())
					failureCollector.record(targetIndex, DalvikAnalysisFailure.FailureKind.INCOMPATIBLE_MERGE,
							"Incompatible register merge at " + orderedRegisters);
			}
		}
		if (changed && queuedIndices.add(targetIndex))
			worklist.add(targetIndex);
	}

	/**
	 * Builds the register state a method starts with.
	 *
	 * @param method
	 * 		Full method reference.
	 * @param member
	 * 		Emitted method metadata and descriptor.
	 * @param code
	 * 		Completed emitted code.
	 * @param entryIndex
	 * 		Index of the entry instruction, used to locate the failure in the source.
	 * @param failureCollector
	 * 		Collector for reported failures.
	 *
	 * @return The entry register state, or {@code null} when the register layout does not match the descriptor,
	 * in which case a failure has been reported.
	 */
	private static @Nullable DalvikRegisterState seedEntryState(@NotNull MethodReference method,
	                                                            @NotNull MethodMember member,
	                                                            @NotNull Code code,
	                                                            int entryIndex,
	                                                            @NotNull FailureCollector failureCollector) {
		boolean isStatic = (member.getAccess() & DalvikModifiers.ACC_STATIC) != 0;
		int registerCount = code.getRegisters();
		int incomingWordCount = isStatic ? 0 : 1;
		List<DescriptorType> parameterTypes = new ArrayList<>(member.getType().parameterTypes().size());
		for (ClassType parameter : member.getType().parameterTypes()) {
			DescriptorType type = DescriptorParser.parseFieldDescriptor(parameter.descriptor());
			parameterTypes.add(type);
			incomingWordCount += Descriptors.isWideType(type) ? 2 : 1;
		}

		// The incoming words must fill exactly the registers the code declares as incoming, otherwise the descriptor
		// and the code cannot be analyzed together.
		if (registerCount < incomingWordCount || incomingWordCount != code.getIn()) {
			failureCollector.record(entryIndex, DalvikAnalysisFailure.FailureKind.INTERNAL_FAILURE, REGISTER_LAYOUT_FAILURE);
			return null;
		}

		// Registers before the parameters start out undefined, since nothing has written them yet.
		int parameterBase = registerCount - incomingWordCount;
		List<DalvikRegisterState.Slot> slots = new ArrayList<>(registerCount);
		for (int register = 0; register < registerCount; register++)
			slots.add(DalvikRegisterState.Undefined.INSTANCE);

		// Seed the parameters into the incoming registers, starting with the receiver for non-static methods.
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
			if (Descriptors.isWideType(parameterType))
				slots.set(register + 1, new DalvikRegisterState.WideTail(register));
			register += Descriptors.isWideType(parameterType) ? 2 : 1;
		}
		return new DalvikRegisterState(slots, null);
	}

	/**
	 * Collector for analysis failures that resolves source locations and drops duplicates.
	 */
	private static final class FailureCollector {
		private final List<Instruction> instructions;
		private final Map<Instruction, ASTInstruction> instructionToSource;
		private final List<DalvikAnalysisFailure> failures;
		private final Set<FailureKey> reportedKeys;

		/**
		 * @param instructions
		 * 		Instructions of the analyzed code.
		 * @param instructionToSource
		 * 		Identity mapping from emitted instructions to source instructions.
		 * @param initial
		 * 		Failures to start from.
		 */
		private FailureCollector(@NotNull List<Instruction> instructions,
		                         @NotNull Map<Instruction, ASTInstruction> instructionToSource,
		                         @NotNull List<DalvikAnalysisFailure> initial) {
			this.instructions = instructions;
			this.instructionToSource = instructionToSource;
			this.failures = new ArrayList<>(initial);
			this.reportedKeys = new HashSet<>();
			for (DalvikAnalysisFailure failure : initial)
				reportedKeys.add(new FailureKey(failure.instructionIndex(), failure.kind(), failure.message()));
		}

		/**
		 * @return Collected failures, in the order they were reported.
		 */
		private @NotNull List<DalvikAnalysisFailure> failures() {
			return failures;
		}

		/**
		 * Records a failure unless an identical one was already reported.
		 *
		 * @param instructionIndex
		 * 		Code index associated with the failure.
		 * @param kind
		 * 		Failure category.
		 * @param message
		 * 		Detail message describing the failure.
		 */
		private void record(int instructionIndex, @NotNull DalvikAnalysisFailure.FailureKind kind, @NotNull String message) {
			if (!reportedKeys.add(new FailureKey(instructionIndex, kind, message)))
				return;

			ASTInstruction source = source(instructionIndex);
			failures.add(new DalvikAnalysisFailure(kind, instructionIndex, message, source == null ? null : source.location()));
		}

		/**
		 * @param instructionIndex
		 * 		Code index to resolve.
		 *
		 * @return Source instruction at that index, or {@code null} when the index is not part of the code.
		 */
		private @Nullable ASTInstruction source(int instructionIndex) {
			return instructionIndex >= 0 && instructionIndex < instructions.size()
					? instructionToSource.get(instructions.get(instructionIndex))
					: null;
		}
	}

	/**
	 * Identity of a reported failure, used to drop duplicates.
	 *
	 * @param instructionIndex
	 * 		Code index associated with the failure.
	 * 		Can be {@code null} when the failure is not associated with a specific instruction.
	 * @param kind
	 * 		Failure category.
	 * @param message
	 * 		Detail message describing the failure.
	 */
	private record FailureKey(@Nullable Integer instructionIndex, @NotNull DalvikAnalysisFailure.FailureKind kind,
	                          @NotNull String message) {}
}
