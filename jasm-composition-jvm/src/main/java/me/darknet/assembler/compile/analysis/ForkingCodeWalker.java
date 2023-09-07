package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.CodeWalker;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.ConditionalJumpInstruction;
import dev.xdark.blw.code.instruction.ImmediateJumpInstruction;
import dev.xdark.blw.code.instruction.SimpleInstruction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ForkingCodeWalker implements CodeWalker, JavaOpcodes {

    private int index;
    private Map<Integer, Frame> frames = new HashMap<>();
    private List<Integer> visited = new ArrayList<>();
    private Deque<Integer> forkQueue = new ArrayDeque<>();
    private Deque<Frame> frameStack = new ArrayDeque<>();

    private final List<CodeElement> backing;
    private final BlwAnalysisEngine engine;

    public ForkingCodeWalker(List<CodeElement> backing, BlwAnalysisEngine engine) {
        this.backing = backing;
        this.engine = engine;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public @Nullable CodeElement element() {
        if(index >= backing.size()) {
            return null;
        }
        return backing.get(index);
    }

    @Override
    public void advance() {
        while (visited.contains(index)) {
            // set the frame to the last frame
            engine.frame(frames.get(index));
            index++;
            System.out.println("Skipping " + index);
        }
        // unvisited index found
        // see if we need to fork

        frames.put(index, engine.frame().copy());

        CodeElement element = element();
        if(element instanceof ImmediateJumpInstruction imm) {
            // no fork needed, instantly jump to the target
            index = backing.indexOf(imm.target());
            System.out.println("Jumping to " + index);
            return;
        } else if(element instanceof ConditionalJumpInstruction cond) {
            // first execute instructions
            engine.execute(cond);
            // push frame
            frameStack.push(engine.frame().copy());

            // fork
            int pos1 = index + 1;
            int pos2 = backing.indexOf(cond.target());

            System.out.println("Forking at " + pos1 + " and " + pos2);

            // fork to pos1
            forkQueue.push(pos1);

            // jump to pos2
            index = pos2;
            return;
        } else if(element instanceof SimpleInstruction sim) {
            switch (sim.opcode()) {
                case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN -> {
                    engine.execute(sim);
                    if (!forkQueue.isEmpty()) {
                        System.out.println("Returning to " + forkQueue.peek());

                        // fork
                        int forkIndex = forkQueue.pop();
                        Frame oldFrame = frameStack.pop();

                        engine.frame(oldFrame);
                        index = forkIndex;
                    } else {
                        visited.add(index++);
                    }

                    return;
                }
            }
        }

        visited.add(index++);
    }

    @Override
    public void set(Label label) {

    }
}
