package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.BranchInstruction;
import dev.xdark.blw.code.instruction.ImmediateJumpInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.Frame;
import me.darknet.assembler.compile.analysis.LocalInfo;
import me.darknet.assembler.compiler.InheritanceChecker;

import java.util.*;

public class AnalysisSimulation implements Simulation<JvmAnalysisEngine, AnalysisSimulation.Info> {
    @Override
    public void execute(JvmAnalysisEngine engine, Info method) {
        Deque<ForkKey> forkQueue = new ArrayDeque<>();

        // Initial frame state holds local variables from parameters.
        // We'll queue up the first instruction as a fork-point.
        Frame initialFrame;
        {
            Frame frame = new Frame();
            int index = 0;
            for (LocalInfo param : method.params) {
                frame.setLocal(index++, param);
            }
            forkQueue.push(new ForkKey(0, frame));
            initialFrame = frame;
            engine.putFrame(0, frame);
        }

        // Next we'll queue the catch blocks as fork-points.
        // We know the stack will only contain a throwable type.
        List<CodeElement> elements = method.method;
        for (TryCatchBlock handler : method.exceptionHandlers) {
            Label h = handler.handler();
            int idx = elements.indexOf(h);
            Frame copy = initialFrame.copy();
            InstanceType type = handler.type();
            if (type == null)
                type = Types.instanceType(Throwable.class);
            copy.push(type);
            forkQueue.push(new ForkKey(idx, copy));
        }

        // Now we'll take each item out of the queue and continue until we hit
        // some code that should create another fork-point (like control flow).
        //
        // Because control flow will allow us to revisit things we already analyzed
        // with a potentially new state, we'll only revisit if the state has changed.
        BitSet visited = new BitSet(elements.size());
        ForkKey key;
        loop:
        while ((key = forkQueue.poll()) != null) {
            int index = key.index;
            Frame frame = key.frame.copy();
            Frame oldFrame = engine.putFrameIfAbsent(index, frame::copy);

            // Only re-check code if the frame state has changed.
            boolean skipVisited;
            if (!oldFrame.equals(frame)) {
                boolean changed = frame.merge(method.checker, oldFrame);
                if (changed)
                    engine.putFrame(index, frame.copy());
                skipVisited = !changed;
            } else {
                skipVisited = true;
            }

            // Move forward in the simulation.
            while (true) {
                if (skipVisited && visited.get(index)) continue loop;

                // Labels are fork-points, queue it and continue to the next point.
                CodeElement element = elements.get(index);
                if (element instanceof Label lbl) {
                    forkQueue.push(new ForkKey(lbl.index() + 1, frame));
                    continue loop;
                }

                // Update the frame in the analyis engine, mark it as visited.
                engine.putFrame(index, frame);
                visited.set(index++);

                // Standard instructions update the frame state
                boolean exit = false;
                if (element instanceof Instruction insn) {
                    ExecutionEngines.execute(engine, insn);
                    int opcode = insn.opcode();
                    exit = (opcode >= JavaOpcodes.IRETURN && opcode <= JavaOpcodes.RETURN)
                            || opcode == JavaOpcodes.ATHROW
                            || insn instanceof ImmediateJumpInstruction;
                }

                // Control flow instructions add new fork-points
                if (element instanceof BranchInstruction bi) {
                    bi.allTargets().forEach(target -> forkQueue.push(new ForkKey(elements.indexOf(target), frame.copy())));
                }

                // If we hit a return instruction, we goto the next fork-point.
                if (exit) {
                    continue loop;
                }
            }
        }
    }

    public record ForkKey(int index, Frame frame) {
    }

    public record Info(InheritanceChecker checker, List<LocalInfo> params, List<CodeElement> method,
                       List<TryCatchBlock> exceptionHandlers) {
    }
}
