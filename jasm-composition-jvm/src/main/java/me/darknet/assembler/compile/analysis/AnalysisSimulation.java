package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.BranchInstruction;
import dev.xdark.blw.code.instruction.ImmediateJumpInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compiler.InheritanceChecker;

import java.util.*;

public class AnalysisSimulation implements Simulation<AnalysisEngine, AnalysisSimulation.Info> {

    @Override
    public void execute(AnalysisEngine engine, Info method) {
        Deque<ForkKey> forkQueue = new ArrayDeque<>();
        Frame initialFrame;
        {
            Frame frame = new Frame();
            int index = 0;
            for (ClassType param : method.params) {
                frame.local(index++, param);
            }
            forkQueue.push(new ForkKey(0, frame));
            initialFrame = frame;
        }
        List<CodeElement> elements = method.method;
        BitSet visited = new BitSet(elements.size());
        for (TryCatchBlock handler : method.exceptionHandlers) {
            Label h = handler.handler();
            int idx = elements.indexOf(h);
            Frame copy = initialFrame.copy();
            InstanceType type = handler.type();
            if (type == null) {
                type = Types.instanceType(Throwable.class);
            }
            copy.push(type);
            forkQueue.push(new ForkKey(idx, copy));
        }
        Map<Integer, Frame> frameMap = new HashMap<>();
        ForkKey key;
        loop:
        while ((key = forkQueue.poll()) != null) {
            int index = key.index;
            Frame frame = key.frame.copy();
            Frame oldFrame = frameMap.putIfAbsent(index, frame.copy());
            boolean checkVisited;
            if (oldFrame != null) {
                boolean noMerge = !frame.merge(method.checker, oldFrame);
                if (noMerge) {
                    continue;
                }
                checkVisited = false;
                frameMap.put(index, frame.copy());
            } else {
                checkVisited = true;
            }
            while (true) {
                if (checkVisited && visited.get(index)) continue loop;
                CodeElement element = elements.get(index);
                if (element instanceof Label lbl) {
                    forkQueue.push(new ForkKey(lbl.index() + 1, frame));
                    continue loop;
                }
                engine.frame(index, frame);
                visited.set(index++);
                boolean exit = false;
                if (element instanceof Instruction insn) {
                    ExecutionEngines.execute(engine, insn);
                    int opcode = insn.opcode();
                    exit = (opcode >= JavaOpcodes.TABLESWITCH && opcode <= JavaOpcodes.RETURN) || opcode == JavaOpcodes.ATHROW || insn instanceof ImmediateJumpInstruction;
                }
                if (element instanceof BranchInstruction bi) {
                    bi.allTargets().forEach(target -> forkQueue.push(new ForkKey(elements.indexOf(target), frame.copy())));
                }
                if (exit) break;
            }
        }
    }

    public record ForkKey(int index, Frame frame) {
    }

    public record Info(InheritanceChecker checker, List<ClassType> params, List<CodeElement> method,
                       List<TryCatchBlock> exceptionHandlers) {
    }

}
