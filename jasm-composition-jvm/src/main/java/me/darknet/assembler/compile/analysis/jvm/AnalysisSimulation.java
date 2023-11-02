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
        ForkKey key;
        loop:
        while ((key = forkQueue.poll()) != null) {
            int index = key.index;
            Frame frame = key.frame.copy();
            Frame oldFrame = engine.putFrameIfAbsent(index, frame::copy);
            boolean checkVisited;
            if (oldFrame != null) {
                boolean noMerge = !frame.merge(method.checker, oldFrame);
                if (noMerge) {
                    continue;
                }
                checkVisited = false;
                engine.putFrame(index, frame.copy());
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
                engine.putFrame(index, frame);
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

    public record Info(InheritanceChecker checker, List<LocalInfo> params, List<CodeElement> method,
                       List<TryCatchBlock> exceptionHandlers) {
    }
}
