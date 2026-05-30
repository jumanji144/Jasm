package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.TryCatchBlock;
import dev.xdark.blw.code.instruction.BranchInstruction;
import dev.xdark.blw.code.instruction.SimpleInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.List;

/**
 * Executes the JVM analysis worklist for a single method.
 */
public class JvmAnalysisRunner implements JavaOpcodes {
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
		typedEngine.bindSession(session);
		try {
			executeBound(typedEngine, session, method);
			return result;
		} finally {
			typedEngine.bindSession(null);
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

		// Seed initial frame and exception handlers.
		Frame initialFrame = seedInitialFrame(method.params());
		session.putFrame(0, initialFrame);
		worklist.add(0);
		seedExceptionHandlers(session, worklist, method, initialFrame);

		// Main analysis loop.
		final List<CodeElement> elements = method.codeElements();
		final int elementCount = elements.size();
		final BitSet visited = new BitSet(elementCount);
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
			while (index < elementCount) {
				Frame oldFrame = frame.copy();
				frame = frame.copy();
				if (index < 0)
					throw new AnalysisException(AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
							"Analysis jumped to invalid range: " + index);

				CodeElement element = elements.get(index);
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
						throw new AnalysisException(element, AnalysisException.FailureKind.FRAME_MERGE, ex);
					}
				}

				// Mark this index as visited, then increment the index.
				// We will record the frame at the incremented index further below.
				// We do not do it immediately since there are some cases with control flow where we will skip putting
				// it at the 'next' frame as denoted by 'index++'.
				int elementIndex = index;
				visited.set(index++);

				// Handle execution of the instruction.
				if (element instanceof Instruction instruction) {
					session.setActiveFrame(elementIndex, frame);
					engine.clearErrorsAt(element);
					try {
						ExecutionEngines.execute(engine, instruction);
					} catch (RuntimeException ex) {
						// Will cover cases like popping off empty stack and implementation bugs in the engine.
						throw new AnalysisException(instruction, AnalysisException.FailureKind.ENGINE_BUG, ex);
					}

					// Abort if control flow is terminal.
					int opcode = instruction.opcode();
					if (opcode == ATHROW || (opcode >= IRETURN && opcode <= RETURN)) {
						// We use the old frame so that it snapshots the state before
						// the return instruction pops off the return value off the stack.
						session.markTerminal(index - 1, oldFrame);
						break;
					}

					// Queue branch targets if this is a branch instruction.
					// This happens after execution since we want to ensure the frame is updated
					// with the effects of the instruction before merging it into the target indices.
					if (element instanceof BranchInstruction branchInstruction) {
						enqueueBranchTargets(session, worklist, checker, elements, frame, visited, branchInstruction, elementCount);
						if (!branchInstruction.hasFallthrough())
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
						throw new AnalysisException(element, AnalysisException.FailureKind.FRAME_MERGE, ex);
					}
				}
			}
		}

		validateReturnInstructions(engine, method.methodType().returnType().descriptor(), method.codeElements());
	}

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
	 * Seeds the initial frames for all exception handlers, which is the same as the initial frame with the addition of the caught exception type on the stack.
	 *
	 * @param session
	 * 		Analysis session to seed frames into.
	 * @param worklist
	 * 		Worklist to seed handler indices into.
	 * @param method
	 * 		Method to seed handlers for.
	 * @param initialFrame
	 * 		Initial frame to copy and add exception types onto for handler frames.
	 *
	 * @throws AnalysisException
	 * 		Thrown when a handler label is invalid or when merging a handler frame results in an error.
	 */
	private void seedExceptionHandlers(@NotNull AnalysisSession<Frame> session, @NotNull AnalysisWorklist worklist,
	                                   @NotNull Info method, @NotNull Frame initialFrame) throws AnalysisException {
		List<CodeElement> elements = method.codeElements();
		for (TryCatchBlock handler : method.exceptionHandlers()) {
			Label handlerLabel = handler.handler();
			int handlerIndex = elements.indexOf(handlerLabel);
			if (handlerIndex < 0) {
				throw new AnalysisException(handlerLabel, AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
						"Try/catch handler label does not exist");
			}

			InstanceType type = handler.type();
			if (type == null)
				type = Types.instanceType(Throwable.class);

			// Queue the handler with a frame that has the caught exception type on the stack.
			Frame frame = initialFrame.copy();
			frame.pushType(type);
			try {
				session.putAndMergeFrame(method.checker(), handlerIndex, frame);
			} catch (FrameMergeException ex) {
				throw new AnalysisException(handlerLabel, AnalysisException.FailureKind.FRAME_MERGE, ex);
			}
			worklist.add(handlerIndex, handlerIndex - Integer.MAX_VALUE);
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
	                                  @NotNull InheritanceChecker checker, @NotNull List<CodeElement> elements,
	                                  @NotNull Frame frame, @NotNull BitSet visited,
	                                  @NotNull BranchInstruction branchInstruction, int elementCount)
			throws AnalysisException {
		for (Label target : branchInstruction.targetsStream().toList()) {
			int targetIndex = elements.indexOf(target);
			if (targetIndex < 0 || targetIndex > elementCount)
				throw new AnalysisException(branchInstruction, AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
						"Target for branch instruction " + branchInstruction + " does not exist");
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
	                                        @NotNull List<CodeElement> elements) {
		for (CodeElement element : elements) {
			if (element instanceof SimpleInstruction(int opcode)) {
				if (opcode == IRETURN && !"ZBCSI".contains(ret))
					engine.warn(element, "Unexpected 'int' return instruction");
				if (opcode == RETURN && !ret.equals("V"))
					engine.warn(element, "Unexpected 'void' return instruction");
				if (opcode == FRETURN && !ret.equals("F"))
					engine.warn(element, "Unexpected 'float' return instruction");
				if (opcode == DRETURN && !ret.equals("D"))
					engine.warn(element, "Unexpected 'float' return instruction");
				if (opcode == LRETURN && !ret.equals("J"))
					engine.warn(element, "Unexpected 'long' return instruction");
				if (opcode == ARETURN && !(ret.charAt(0) == 'L' || ret.charAt(0) == '['))
					engine.warn(element, "Unexpected 'object' return instruction");
			}
		}
	}

	public record Info(@NotNull InheritanceChecker checker, @NotNull MethodType methodType, @NotNull List<Local> params,
	                   @NotNull List<CodeElement> codeElements, @NotNull List<TryCatchBlock> exceptionHandlers) {}
}
