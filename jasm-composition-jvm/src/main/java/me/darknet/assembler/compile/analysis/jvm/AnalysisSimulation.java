package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.BranchInstruction;
import dev.xdark.blw.code.instruction.ConditionalJumpInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;
import dev.xdark.blw.simulation.SimulationException;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.Frame;
import me.darknet.assembler.compile.analysis.LocalInfo;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AnalysisSimulation implements Simulation<JvmAnalysisEngine, AnalysisSimulation.Info>, JavaOpcodes {
	private static final int MAX_QUEUE = 2048;

	@Override
	public void execute(JvmAnalysisEngine engine, AnalysisSimulation.Info method) throws SimulationException {
		final InheritanceChecker checker = method.checker();
		final ForkQueue forkQueue = new ForkQueue(checker);

		// Initial frame state holds local variables from parameters.
		// We'll queue up the first instruction as a fork-point.
		final Frame initialFrame;
		{
			initialFrame = new Frame();
			int index = 0;
			for (LocalInfo param : method.params()) {
				int idx = index++;
				if (param == null) continue; // top
				initialFrame.setLocal(idx, param);
			}
			forkQueue.add(new ForkKey(0, initialFrame));
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

			Frame frame = initialFrame.copy();
			frame.push(type);
			forkQueue.add(new ForkKey(handlerIndex, frame));
		}

		// Populate initial frame states for existing fork-keys.
		for (ForkKey fork : forkQueue) {
			engine.putFrame(fork.index(), fork.frame());
		}

		// Visit all queued fork-points. As we execute the code we may discover new points which to visit.
		// We will continue to poll the next item and execute sequentially until we see the frame merging
		// at fork-points results in no-changes.
		ForkKey fork;
		final BitSet visited = new BitSet(elementCount);
		while ((fork = forkQueue.next()) != null) {
			// Exit if we're getting out of control.
			if (forkQueue.size() > MAX_QUEUE)
				throw new SimulationException("Exceeded max queue size in stack simulation: " + MAX_QUEUE);

			// Get the initial state at this fork-point.
			int index = fork.index;
			Frame frame = engine.getFrame(index);

			// Execute sequentially until hitting a fork-point with forcefully directed (or terminating) flow.
			while (index < elementCount) {
				frame = frame.copy();
				CodeElement element = elements.get(index);

				Frame existingFrame = engine.getFrame(index);
				if (existingFrame != null) {
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
				}

				// Mark this index as visited, then increment the index.
				// We will be putting the frame at the 'next' index since the frame after is what sees
				// the results of the prior instruction's execution. The 'frame' instance is shared below
				// in the instruction execution logic.
				visited.set(index++);
				engine.putFrame(index, frame);

				if (element instanceof BranchInstruction bi) {
					ExecutionEngines.execute(engine, bi);

					// Create fork-points for all target labels.
					for (Label target : bi.targetsStream().toList()) {
						int targetIndex = elements.indexOf(target);
						if (targetIndex < 0 || targetIndex > elementCount)
							throw new SimulationException("Target for branch instruction " + element + " does not exist");
						forkQueue.add(new ForkKey(targetIndex, frame));
						boolean changed = engine.mergeInto(targetIndex, frame, checker);
						if (changed) visited.clear(targetIndex);
					}

					// Break if control flow does not have fall-through case.
					if (!(bi instanceof ConditionalJumpInstruction))
						break;
				} else if (element instanceof Instruction insn) {
					ExecutionEngines.execute(engine, insn);

					// Abort if control flow is terminal.
					int opcode = insn.opcode();
					if (opcode == ATHROW || (opcode >= IRETURN && opcode <= RETURN))
						break;
				}
			}
		}
	}

	private static class ForkQueue implements Iterable<ForkKey> {
		private final NavigableMap<Integer, ForkKey> forkQueue = new TreeMap<>();
		private final InheritanceChecker checker;

		public ForkQueue(@NotNull InheritanceChecker checker) {
			this.checker = checker;
		}

		public void add(@NotNull ForkKey key) throws SimulationException {
			ForkKey oldForkKey = forkQueue.get(key.index);
			if (oldForkKey != null) {
				Frame frameA = key.frame().copy();
				Frame frameB = oldForkKey.frame();
				frameA.merge(checker, frameB);
				key = new ForkKey(key.index, frameA);
			}
			forkQueue.put(key.index, key);
		}

		public int size() {
			return forkQueue.size();
		}

		@Nullable
		public ForkKey next() {
			var entry = forkQueue.pollLastEntry();
			if (entry == null) return null;
			return entry.getValue();
		}

		@NotNull
		@Override
		public Iterator<ForkKey> iterator() {
			return forkQueue.values().iterator();
		}
	}

	private record ForkKey(int index, @NotNull Frame frame) implements Comparable<ForkKey> {
		@Override
		public int compareTo(@NotNull AnalysisSimulation.ForkKey other) {
			return Integer.compare(index, other.index);
		}
	}

	public record Info(InheritanceChecker checker, List<LocalInfo> params, List<CodeElement> method,
					   List<TryCatchBlock> exceptionHandlers) {
	}
}
