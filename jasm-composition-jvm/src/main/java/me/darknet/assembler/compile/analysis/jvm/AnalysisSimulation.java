package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.BranchInstruction;
import dev.xdark.blw.code.instruction.ConditionalJumpInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.*;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class AnalysisSimulation<L extends Local, S extends StackEntry> implements Simulation<JvmAnalysisEngine, AnalysisParams>, JavaOpcodes {
	private static final int MAX_QUEUE = 2048;

	private final Supplier<AbstractFrame<L,S>> frameSupplier;
	private final Function<Local,L> localGenerifier;

	public AnalysisSimulation(Supplier<AbstractFrame<L, S>> frameSupplier, Function<Local, L> localGenerifier) {
		this.frameSupplier = frameSupplier;
		this.localGenerifier = localGenerifier;
	}

	@Override
	public void execute(JvmAnalysisEngine engine, AnalysisParams method) throws AnalysisException {
		final InheritanceChecker checker = method.checker();
		final ForkQueue forkQueue = new ForkQueue(checker);

		// Initial frame state holds local variables from parameters.
		// We'll queue up the first instruction as a fork-point.
		final AbstractFrame<L,S> initialFrame;
		{
			initialFrame = frameSupplier.get();
			int index = 0;
			for (Local param : method.params()) {
				int idx = index++;
				if (param == null) continue; // top
				initialFrame.setLocal(idx, localGenerifier.apply(param));
			}
			try {
				forkQueue.add(new ForkKey<>(0, initialFrame));
			} catch (FrameMergeException e) {
				throw new AnalysisException(e, "Failed allocating initial frame");
			}
		}

		// Next we'll queue the catch blocks as fork-points.
		// We know the stack will only contain a throwable type.
		final List<CodeElement> elements = method.method();
		final int elementCount = elements.size();
		for (TryCatchBlock handler : method.exceptionHandlers()) {
			Label handlerLabel = handler.handler();
			int handlerIndex = elements.indexOf(handlerLabel);
			InstanceType type = handler.type();
			if (type == null)
				type = Types.instanceType(Throwable.class);

			AbstractFrame<L,S> frame = initialFrame.copy();
			frame.push(type);
			try {
				forkQueue.add(new ForkKey<>(handlerIndex, frame));
			} catch (FrameMergeException ex) {
				throw new AnalysisException(ex, "Failed allocating handler frame");
			}
		}

		// Populate initial frame states for existing fork-keys.
		for (ForkKey<L,S> fork : forkQueue) {
			engine.putFrame(fork.index(), fork.frame());
		}

		// Visit all queued fork-points. As we execute the code we may discover new points which to visit.
		// We will continue to poll the next item and execute sequentially until we see the frame merging
		// at fork-points results in no-changes.
		ForkKey<L,S> fork;
		final BitSet visited = new BitSet(elementCount);
		while ((fork = forkQueue.next()) != null) {
			// Exit if we're getting out of control.
			if (forkQueue.size() > MAX_QUEUE)
				throw new AnalysisException("Exceeded max queue size in stack simulation: " + MAX_QUEUE);

			// Get the initial state at this fork-point.
			int index = fork.index();
			SimpleFrame frame = engine.getFrame(index);

			// Execute sequentially until hitting a fork-point with forcefully directed (or terminating) flow.
			while (index < elementCount) {
				frame = frame.copy();
				CodeElement element = elements.get(index);

				SimpleFrame existingFrame = engine.getFrame(index);
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
						if (!changed && visited.get(index))
							break;
					} catch (FrameMergeException ex) {
						throw new AnalysisException(element, ex);
					}
				}

				// Mark this index as visited, then increment the index.
				// We will be putting the frame at the 'next' index since the frame after is what sees
				// the results of the prior instruction's execution. The 'frame' instance is shared below
				// in the instruction execution logic.
				visited.set(index++);
				engine.putFrame(index, frame);

				// Handle execution of the instruction.
				if (element instanceof Instruction insn) {
					try {
						ExecutionEngines.execute(engine, insn);
					} catch (Throwable t) {
						// Will cover cases like popping off empty stack and implementation bugs in the engine.
						throw new AnalysisException(insn, t);
					}

					// Abort if control flow is terminal.
					int opcode = insn.opcode();
					if (opcode == ATHROW || (opcode >= IRETURN && opcode <= RETURN))
						break;

					// Create fork-points for all target labels of branching instructions.
					if (element instanceof BranchInstruction bi) {
						for (Label target : bi.targetsStream().toList()) {
							int targetIndex = elements.indexOf(target);
							if (targetIndex < 0 || targetIndex > elementCount)
								throw new AnalysisException(bi, "Target for branch instruction " + bi + " does not exist");
							try {
								boolean changed = engine.mergeInto(targetIndex, frame, checker);
								if (changed) {
									forkQueue.add(new ForkKey(targetIndex, frame));
									visited.clear(targetIndex);
								}
							} catch (FrameMergeException ex) {
								throw new AnalysisException(bi, ex);
							}
						}

						// Break if control flow does not have fall-through case.
						if (!(bi instanceof ConditionalJumpInstruction))
							break;
					}
				}
			}
		}
	}

	private class ForkQueue implements Iterable<ForkKey<L,S>> {
		private final NavigableMap<Integer, ForkKey<L,S>> forkQueue = new TreeMap<>();
		private final InheritanceChecker checker;

		public ForkQueue(@NotNull InheritanceChecker checker) {
			this.checker = checker;
		}

		public void add(@NotNull ForkKey<L,S> key) throws FrameMergeException {
			ForkKey<L,S> oldForkKey = forkQueue.get(key.index());
			if (oldForkKey != null) {
				AbstractFrame<L,S> frameA = key.frame().copy();
				AbstractFrame<L,S> frameB = oldForkKey.frame();
				frameA.merge(checker, frameB);
				key = new ForkKey<>(key.index(), frameA);
			}
			forkQueue.put(key.index(), key);
		}

		public int size() {
			return forkQueue.size();
		}

		@Nullable
		public ForkKey<L,S> next() {
			var entry = forkQueue.pollLastEntry();
			if (entry == null) return null;
			return entry.getValue();
		}

		@NotNull
		@Override
		public Iterator<ForkKey<L,S>> iterator() {
			return forkQueue.values().iterator();
		}
	}

}
