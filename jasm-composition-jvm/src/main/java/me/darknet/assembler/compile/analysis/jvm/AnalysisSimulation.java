package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compiler.InheritanceChecker;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.BranchInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AnalysisSimulation implements Simulation<JvmAnalysisEngine<Frame>, AnalysisSimulation.Info>, JavaOpcodes {
    private static final int MAX_QUEUE = 2048;

    private final FrameOps<Frame> frameOps;

    @SuppressWarnings("unchecked")
    public AnalysisSimulation(FrameOps<?> frameOps) {
        this.frameOps = (FrameOps<Frame>) frameOps;
    }

    @Override
    public void execute(JvmAnalysisEngine<Frame> engine, AnalysisSimulation.Info method) throws AnalysisException {
        final InheritanceChecker checker = method.checker();
        final ForkQueue forkQueue = new ForkQueue(checker);

        // Initial frame state holds local variables from parameters.
        // We'll queue up the first instruction as a fork-point.
        final Frame initialFrame;
        {
            initialFrame = frameOps.newEmptyFrame();
            int index = 0;
            for (Local param : method.params()) {
                int idx = index++;
                if (param == null)
                    continue; // top
                frameOps.setFrameLocal(initialFrame, idx, param);
            }
            try {
                forkQueue.add(new ForkKey(0, initialFrame));
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

            Frame frame = initialFrame.copy();
            frame.pushType(type);
            try {
                forkQueue.add(new ForkKey(handlerIndex, frame));
            } catch (FrameMergeException ex) {
                throw new AnalysisException(ex, "Failed allocating handler frame");
            }
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
                throw new AnalysisException("Exceeded max queue size in stack simulation: " + MAX_QUEUE);

            // Get the initial state at this fork-point.
            int index = fork.index;
            Frame frame = engine.getFrame(index);
            if (frame == null)
                throw new AnalysisException("No frame at index " + index);

            // For tracking a change coming from a previous iteration / flow point.
            boolean mergeChangedPreviously = false;

            // Execute sequentially until hitting a fork-point with forcefully directed (or terminating) flow.
            while (index < elementCount) {
                Frame oldFrame = frame.copy();
                frame = frame.copy();
                CodeElement element = elements.get(index);

                Frame existingFrame = engine.getFrame(index);
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
                        throw new AnalysisException(element, ex);
                    }
                }

                // Mark this index as visited, then increment the index.
                // We will record the frame at the incremented index further below.
                // We do not do it immediately since there are some cases with control flow where we will skip putting
                // it at the 'next' frame as denoted by 'index++'.
                visited.set(index++);

                // Handle execution of the instruction.
                if (element instanceof Instruction insn) {
                    try {
                        engine.setActiveFrame(frame);
                        ExecutionEngines.execute(engine, insn);
                    } catch (Throwable t) {
                        // Will cover cases like popping off empty stack and implementation bugs in the engine.
                        throw new AnalysisException(insn, t);
                    }

                    // Abort if control flow is terminal.
                    int opcode = insn.opcode();
                    if (opcode == ATHROW || (opcode >= IRETURN && opcode <= RETURN)) {
                        // We use the old frame so that it snapshots the state before
                        // the return instruction pops off the return value off the stack.
                        engine.markTerminal(index - 1, oldFrame);
                        break;
                    }

                    // Create fork-points for all target labels of branching instructions.
                    if (element instanceof BranchInstruction bi) {
                        for (Label target : bi.targetsStream().toList()) {
                            int targetIndex = elements.indexOf(target);
                            if (targetIndex < 0 || targetIndex > elementCount)
                                throw new AnalysisException(bi, "Target for branch instruction " + bi + " does not exist");
                            try {
                                boolean shouldVisitTarget;
                                Frame targetFrame = engine.getFrame(targetIndex);
                                if (targetFrame == null) {
                                    // Not seen before, should visit as fork-point.
                                    engine.putFrame(targetIndex, frame);
                                    shouldVisitTarget = true;
                                } else {
                                    // We've already created a frame for that index previously.
                                    // We only want to revisit it if merging the current frame into the target's
                                    // will result in a change to the target frame's state.
                                    Frame mergeTarget = targetFrame.copy();
                                    shouldVisitTarget = mergeTarget.merge(checker, frame);
                                    if (shouldVisitTarget)
                                        engine.putAndMergeFrame(checker, targetIndex, mergeTarget);
                                }

                                // Queue the fork-point if needed.
                                if (shouldVisitTarget) {
                                    forkQueue.add(new ForkKey(targetIndex, frame));
                                    visited.clear(targetIndex);
                                }
                            } catch (FrameMergeException ex) {
                                throw new AnalysisException(bi, ex);
                            }
                        }

                        // Break if control flow does not have fall-through case.
                        if (!bi.hasFallthrough())
                            break;
                    }
                    try {
                        // Either a non-branching instruction, or a conditional jump with fall-through,
                        // thus we want to record the frame.
                        //
                        // Additionally, if this results in a merge change we want the next iteration to be made
                        // aware of this so that it won't pre-maturely abort.
                        mergeChangedPreviously = engine.putAndMergeFrame(checker, index, frame);
                    } catch (FrameMergeException ex) {
                        throw new AnalysisException(element, ex);
                    }
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

        public void add(@NotNull ForkKey key) throws FrameMergeException {
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
            if (entry == null)
                return null;
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

    public record Info(
            InheritanceChecker checker, List<Local> params, List<CodeElement> method,
            List<TryCatchBlock> exceptionHandlers
    ) {
    }
}
