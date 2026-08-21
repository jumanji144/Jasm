package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValuedLocal;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Executes the JVM analysis worklist for a single method.
 */
public class JvmAnalysisRunner implements Opcodes {
	private static final int MAX_QUEUE = 2048;

	private final FrameOps<Frame> frameOps;

	/**
	 * @param frameOps
	 * 		Frame operations implementation to use for this analysis runner.
	 */
	@SuppressWarnings("unchecked")
	public JvmAnalysisRunner(FrameOps<?> frameOps) {
		this.frameOps = (FrameOps<Frame>) frameOps;
	}

	/**
	 * Executes the analysis for a single method, returning the result.
	 *
	 * @param engine
	 * 		The engine to execute analysis with.
	 * @param method
	 * 		The method to analyze.
	 * @param result
	 * 		The result object to populate with analysis results.
	 *
	 * @return The provided result object, populated with analysis results.
	 *
	 * @throws AnalysisException
	 * 		Thrown when analysis fails due to invalid code.
	 */
	public MethodAnalysisResult execute(@NotNull JvmAnalysisEngine<?> engine, @NotNull Info method,
	                                    @NotNull MethodAnalysisResult result) throws AnalysisException {
		@SuppressWarnings("unchecked")
		JvmAnalysisEngine<Frame> typedEngine = (JvmAnalysisEngine<Frame>) engine;
		AnalysisSession<Frame> session = new AnalysisSession<>(result);
		typedEngine.setResult(result);
		typedEngine.setMethodDetails(Type.getReturnType(method.methodType().getDescriptor()), method.owner(), method.name(), method.access());
		typedEngine.setSession(session);
		try {
			executeBound(typedEngine, session, method);
			return result;
		} finally {
			typedEngine.setSession(null);
		}
	}

	/**
	 * Internal method to execute analysis with a bound session.
	 *
	 * @param engine
	 * 		The engine to execute analysis with.
	 * @param session
	 * 		The session to execute analysis with.
	 * @param method
	 * 		The method to analyze.
	 *
	 * @throws AnalysisException
	 * 		Thrown when analysis fails due to invalid code.
	 */
	private void executeBound(@NotNull JvmAnalysisEngine<Frame> engine, @NotNull AnalysisSession<Frame> session,
	                          @NotNull Info method) throws AnalysisException {
		final InheritanceChecker checker = method.checker();
		final AnalysisWorklist worklist = new AnalysisWorklist();

		// Seed only the standard method entry.
		//
		// Exception handlers are reached from every reachable instruction inside their protected ranges below.
		Frame initialFrame = seedInitialFrame(method.params());

		// If this is a constructor, we need to mark the receiver as uninitialized so that it can be tracked
		// and validated for proper initialization before use.
		if ("<init>".equals(method.name())
				&& (method.access() & ACC_STATIC) == 0
				&& !method.params().isEmpty()
				&& !method.owner().isEmpty()) {
			// Get the receiver local variable and mark it as uninitialized.
			Local receiver = method.params().getFirst();
			Type owner = Type.getObjectType(method.owner());
			Type marker = engine.newUninitializedType(owner);
			if (initialFrame instanceof TypedFrame typedFrame)
				typedFrame.setLocal(receiver.index(), new Local(receiver.index(), receiver.name(), marker));
			else if (initialFrame instanceof ValuedFrame valuedFrame)
				valuedFrame.setLocal(receiver.index(), new ValuedLocal(receiver.index(), receiver.name(),
						new Value.UninitializedObjectValue(marker, owner)));
		}
		session.putFrame(0, initialFrame);
		worklist.add(0);

		// Main analysis loop.
		final List<AbstractInsnNode> instructions = method.instructions();
		final int instructionCount = instructions.size();
		final BitSet visited = new BitSet(instructionCount);
		AnalysisWorklist.Entry entry;
		while ((entry = worklist.next()) != null) {
			// Exit if we're getting out of control.
			if (worklist.size() > MAX_QUEUE)
				throw new AnalysisException(AnalysisException.FailureKind.QUEUE_OVERFLOW,
						"Exceeded max queue size in stack simulation: " + MAX_QUEUE);

			// Get the initial state at this index.
			int index = entry.index();
			Frame frame = session.getFrame(index);
			if (frame == null)
				throw new AnalysisException(AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
						"No frame at index " + index);

			// For tracking a change coming from a previous iteration / flow point.
			boolean mergeChangedPreviously = false;

			// Execute sequentially until hitting a fork-point with forcefully directed (or terminating) flow.
			while (index < instructionCount) {
				Frame oldFrame = frame.copy();
				frame = frame.copy();
				if (index < 0)
					throw new AnalysisException(AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
							"Analysis jumped to invalid range: " + index);

				AbstractInsnNode instruction = instructions.get(index);
				Frame existingFrame = session.getFrame(index);
				if (existingFrame != null) {
					try {
						// We need to merge our frame with the existing one to ensure types in the
						// local variable table and stack are common to all execution paths.
						//
						// We also check for the existing frame merging with ours to see if we need
						// to overwrite the existing frame with a more up-to-date state.
						boolean changed = frame.merge(checker, existingFrame)
								|| existingFrame.copy().merge(checker, frame);

						// We can continue the sequential execution if the code has already been visited
						// and there were no changes in the merge process.
						if (!mergeChangedPreviously && !changed && visited.get(index))
							break;
					} catch (FrameMergeException ex) {
						throw new AnalysisException(instruction, AnalysisException.FailureKind.FRAME_MERGE, ex);
					}
				}

				// Mark this index as visited, then increment the index.
				// We will record the frame at the incremented index further below.
				// We do not do it immediately since there are some cases with control flow where we will skip putting
				// it at the 'next' frame as denoted by 'index++'.
				int elementIndex = index;
				visited.set(index++);

				// Handle execution of the instruction.
				if (isExecutable(instruction)) {
					session.setActiveFrame(elementIndex, frame);
					engine.clearErrorsAt(instruction);
					enqueueExceptionHandlers(session, worklist, checker, method, elementIndex, oldFrame, visited);
					try {
						engine.execute(instruction);
					} catch (RuntimeException ex) {
						// Will cover cases like popping off empty stack and implementation bugs in the engine.
						throw new AnalysisException(instruction, AnalysisException.FailureKind.ENGINE_BUG, ex);
					}

					// Abort if control flow is terminal.
					int opcode = instruction.getOpcode();
					if (opcode == ATHROW || (opcode >= IRETURN && opcode <= RETURN)) {
						// We use the old frame so that it snapshots the state before
						// the return instruction pops off the return value off the stack.
						session.markTerminal(index - 1, oldFrame);
						break;
					}

					// Queue branch targets if this is a branch instruction.
					// This happens after execution since we want to ensure the frame is updated
					// with the effects of the instruction before merging it into the target indices.
					if (isBranch(instruction)) {
						enqueueBranchTargets(session, worklist, checker, instructions, frame, visited, instruction, instructionCount);
						if (!hasFallthrough(instruction))
							break;
					}

					try {
						// Either a non-branching instruction, or a conditional jump with fall-through,
						// thus we want to record the frame.
						//
						// Additionally, if this results in a merge change we want the next iteration to be made
						// aware of this so that it won't pre-maturely abort.
						mergeChangedPreviously = session.putAndMergeFrame(checker, index, frame);
					} catch (FrameMergeException ex) {
						throw new AnalysisException(instruction, AnalysisException.FailureKind.FRAME_MERGE, ex);
					}
				}
			}
		}

		validateReturnInstructions(engine, Type.getReturnType(method.methodType().getDescriptor()).getDescriptor(),
				method.instructions(), visited);
	}

	/**
	 * Seeds the initial frame for the method.
	 *
	 * @param params
	 * 		Parameters to seed into the initial frame.
	 *
	 * @return The seeded initial frame.
	 */
	private Frame seedInitialFrame(@NotNull List<Local> params) {
		Frame initialFrame = frameOps.newEmptyFrame();
		int index = 0;
		for (Local param : params) {
			int idx = index++;
			if (param == null)
				continue;
			frameOps.setFrameLocal(initialFrame, idx, param);
		}
		return initialFrame;
	}

	/**
	 * Seeds the initial frames for all exception handlers.
	 *
	 * @param session
	 * 		Analysis session to seed frames into.
	 * @param worklist
	 * 		Worklist to seed handler indices into.
	 * @param checker
	 * 		Inheritance checker to use for frame merging.
	 * @param method
	 * 		Method to seed handlers for.
	 * @param instructionIndex
	 * 		Index of the instruction that is throwing an exception.
	 * @param throwingFrame
	 * 		Frame at the instruction that is throwing an exception.
	 * @param visited
	 * 		BitSet tracking which indices have been visited before.
	 *
	 * @throws AnalysisException
	 * 		Thrown when a handler label is invalid or when merging a handler frame results in an error.
	 */
	private void enqueueExceptionHandlers(@NotNull AnalysisSession<Frame> session, @NotNull AnalysisWorklist worklist,
	                                      @NotNull InheritanceChecker checker, @NotNull Info method, int instructionIndex,
	                                      @NotNull Frame throwingFrame, @NotNull BitSet visited) throws AnalysisException {
		List<AbstractInsnNode> elements = method.instructions();
		for (TryCatchBlockNode handler : method.exceptionHandlers()) {
			int startIndex = elements.indexOf(handler.start);
			int endIndex = elements.indexOf(handler.end);
			int handlerIndex = elements.indexOf(handler.handler);
			if (startIndex < 0 || endIndex < 0 || handlerIndex < 0) {
				throw new AnalysisException(handler.handler, AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
						"Try/catch range or handler label does not exist");
			}
			if (instructionIndex < startIndex || instructionIndex >= endIndex)
				continue;

			// An exceptional edge preserves locals from immediately before the throwing instruction and replaces
			// the operand stack with the exception. So we want to:
			//  - Copy the frame from immediately before the throwing instruction.
			//  - Clear the operand stack.
			//  - Push the exception type onto the operand stack.
			Type type = handler.type == null ? JvmTypeUtils.type(Throwable.class) : Type.getObjectType(handler.type);
			Frame frame = throwingFrame.copy();
			if (frame instanceof TypedFrame typedFrame)
				typedFrame.getStack().clear();
			else if (frame instanceof ValuedFrame valuedFrame)
				valuedFrame.getStack().clear();
			frame.pushType(type);

			// Queue the handler index for visiting if the merge results in a change to the handler frame's state.
			try {
				Frame existing = session.getFrame(handlerIndex);
				boolean changed = existing == null;
				if (existing == null) {
					session.putFrame(handlerIndex, frame);
				} else {
					Frame merged = existing.copy();
					changed = merged.merge(checker, frame);
					if (changed)
						session.putFrame(handlerIndex, merged);
				}

				// If the merge resulted in a change to the handler frame's state, we need to queue it for visiting.
				if (changed) {
					visited.clear(handlerIndex);
					worklist.add(handlerIndex, handlerIndex - Integer.MAX_VALUE);
				}
			} catch (FrameMergeException ex) {
				throw new AnalysisException(handler.handler, AnalysisException.FailureKind.FRAME_MERGE, ex);
			}
		}
	}

	/**
	 * Queues the targets of a branch instruction, merging the current frame into the target frames
	 * and marking them for visiting if the merge resulted in a change to the target frame's state.
	 *
	 * @param session
	 * 		Analysis session to read and write frames from.
	 * @param worklist
	 * 		Worklist to queue target indices into.
	 * @param checker
	 * 		Inheritance checker to use for frame merging.
	 * @param elements
	 * 		List of code elements to resolve branch targets against.
	 * @param frame
	 * 		Current frame to merge into targets.
	 * @param visited
	 * 		BitSet tracking which indices have been visited before.
	 * @param branchInstruction
	 * 		Branch instruction to enqueue targets for.
	 * @param elementCount
	 * 		Number of code elements, used for validating target indices.
	 *
	 * @throws AnalysisException
	 * 		Thrown when a target label is invalid or when merging into a target frame results in an error.
	 */
	private void enqueueBranchTargets(@NotNull AnalysisSession<Frame> session, @NotNull AnalysisWorklist worklist,
	                                  @NotNull InheritanceChecker checker, @NotNull List<AbstractInsnNode> elements,
	                                  @NotNull Frame frame, @NotNull BitSet visited,
	                                  @NotNull AbstractInsnNode branchInstruction, int elementCount)
			throws AnalysisException {
		for (LabelNode target : branchTargets(branchInstruction)) {
			int targetIndex = elements.indexOf(target);
			if (targetIndex < 0 || targetIndex > elementCount)
				throw new AnalysisException(branchInstruction, AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
						"Target for branch instruction does not exist");
			try {
				boolean shouldVisitTarget;
				Frame targetFrame = session.getFrame(targetIndex);
				if (targetFrame == null) {
					// Not seen before, should queue unconditionally.
					session.putFrame(targetIndex, frame);
					shouldVisitTarget = true;
				} else {
					// We've already created a frame for that index previously.
					// We only want to revisit it if merging the current frame into the target's
					// will result in a change to the target frame's state.
					Frame mergeTarget = targetFrame.copy();
					shouldVisitTarget = mergeTarget.merge(checker, frame);
					if (shouldVisitTarget)
						session.putAndMergeFrame(checker, targetIndex, mergeTarget);
				}

				// Queue the target if we haven't visited it before,
				// or if the merge resulted in a change to the target frame's state.
				if (shouldVisitTarget) {
					worklist.add(targetIndex);
					visited.clear(targetIndex);
				}
			} catch (FrameMergeException ex) {
				throw new AnalysisException(branchInstruction, AnalysisException.FailureKind.FRAME_MERGE, ex);
			}
		}
	}

	private void validateReturnInstructions(@NotNull JvmAnalysisEngine<Frame> engine, @NotNull String ret,
	                                        @NotNull List<AbstractInsnNode> elements, @NotNull BitSet reachable) {
		for (int index = 0; index < elements.size(); index++) {
			if (!reachable.get(index))
				continue;
			AbstractInsnNode instruction = elements.get(index);
			int opcode = instruction.getOpcode();
			if (opcode == IRETURN && !"ZBCSI".contains(ret))
				engine.warn(instruction, "Unexpected 'int' return instruction");
			if (opcode == RETURN && !ret.equals("V"))
				engine.warn(instruction, "Unexpected 'void' return instruction");
			if (opcode == FRETURN && !ret.equals("F"))
				engine.warn(instruction, "Unexpected 'float' return instruction");
			if (opcode == DRETURN && !ret.equals("D"))
				engine.warn(instruction, "Unexpected 'double' return instruction");
			if (opcode == LRETURN && !ret.equals("J"))
				engine.warn(instruction, "Unexpected 'long' return instruction");
			if (opcode == ARETURN && !(ret.charAt(0) == 'L' || ret.charAt(0) == '['))
				engine.warn(instruction, "Unexpected 'object' return instruction");
		}
	}

	private static boolean isExecutable(@NotNull AbstractInsnNode node) {
		return !(node instanceof LabelNode || node instanceof LineNumberNode || node instanceof FrameNode);
	}

	private static boolean isBranch(@NotNull AbstractInsnNode node) {
		return node instanceof JumpInsnNode || node instanceof LookupSwitchInsnNode || node instanceof TableSwitchInsnNode;
	}

	private static boolean hasFallthrough(@NotNull AbstractInsnNode node) {
		if (node instanceof JumpInsnNode jumpInsnNode)
			return jumpInsnNode.getOpcode() != GOTO;
		return false;
	}

	private static @NotNull List<LabelNode> branchTargets(@NotNull AbstractInsnNode node) {
		switch (node) {
			case JumpInsnNode jumpInsnNode -> {
				return List.of(jumpInsnNode.label);
			}
			case LookupSwitchInsnNode lookupSwitchInsnNode -> {
				List<LabelNode> labels = new ArrayList<>(lookupSwitchInsnNode.labels);
				labels.add(lookupSwitchInsnNode.dflt);
				return labels;
			}
			case TableSwitchInsnNode tableSwitchInsnNode -> {
				List<LabelNode> labels = new ArrayList<>(tableSwitchInsnNode.labels);
				labels.add(tableSwitchInsnNode.dflt);
				return labels;
			}
			default -> {}
		}
		return List.of();
	}

	/**
	 * Method state wrapper.
	 *
	 * @param checker
	 * 		Inheritance resolution for frame merging of different class types.
	 * @param owner
	 * 		Name of the class that owns the method.
	 * @param name
	 * 		Method name.
	 * @param access
	 * 		Method access flags.
	 * @param methodType
	 * 		Type descriptor of the method.
	 * @param params
	 * 		Method parameters.
	 * @param instructions
	 * 		Method code.
	 * @param exceptionHandlers
	 * 		Method try-catch blocks.
	 */
	public record Info(@NotNull InheritanceChecker checker,
	                   @NotNull String owner,
	                   @NotNull String name,
	                   int access,
	                   @NotNull Type methodType,
	                   @NotNull List<Local> params,
	                   @NotNull List<AbstractInsnNode> instructions,
	                   @NotNull List<TryCatchBlockNode> exceptionHandlers) {}
}
