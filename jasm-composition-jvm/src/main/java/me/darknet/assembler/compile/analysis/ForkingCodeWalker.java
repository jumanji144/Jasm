package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.ConditionalJumpInstruction;
import dev.xdark.blw.code.instruction.ImmediateJumpInstruction;
import dev.xdark.blw.code.instruction.SimpleInstruction;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ForkingCodeWalker implements CodeWalker, JavaOpcodes {

    private int index;
    Map<Integer, Frame> frames = new HashMap<>();
    private final List<Integer> visited = new ArrayList<>();
    private final Deque<Integer> forkQueue = new ArrayDeque<>();

    private final List<CodeElement> backing;
    private final List<TryCatchBlock> exceptionHandlers;
    private final AnalysisEngine engine;
    private final InheritanceChecker inheritanceChecker;

    public ForkingCodeWalker(List<CodeElement> backing, List<TryCatchBlock> exceptionHandlers,
                             AnalysisEngine engine, InheritanceChecker inheritanceChecker) {
        this.backing = backing;
        this.exceptionHandlers = exceptionHandlers;
        this.engine = engine;
        this.inheritanceChecker = inheritanceChecker;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public @Nullable CodeElement element() {
        if (index >= backing.size()) {
            return null;
        }
        return backing.get(index);
    }

    public TryCatchBlock getExceptionHandler() {
        // algorithm is according to Method::fast_exception_handler_bci_for, in jdk source:
        // https://https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/hotspot/src/share/vm/oops/method.cpp#L180
        ClassType exKlass = engine.frame().peek();
        for (TryCatchBlock exceptionHandler : exceptionHandlers) {
            int begBci = backing.indexOf(exceptionHandler.start());
            int endBci = backing.indexOf(exceptionHandler.end());

            if(begBci <= index && index < endBci) {
                // further investigate the exception type
                InstanceType exceptionType = exceptionHandler.type();
                if(exceptionType == null) { // catch all block
                    return exceptionHandler;
                } else if(exKlass == null) { // any catch block needs to catch a null exception
                    return exceptionHandler;
                } else {
                    // check if the class is a subtype of exceptionType
                    InstanceType klass = (InstanceType) exKlass; // upwards qualify
                    if(inheritanceChecker.isSubclassOf(klass.internalName(), exceptionType.internalName())) {
                        return exceptionHandler;
                    }
                }
            }
        }
        return null;
    }

    void forkOrExit() {
        if (!forkQueue.isEmpty()) {
            // fork
            int forkIndex = forkQueue.pop();
            Frame oldFrame = frames.get(forkIndex - 1).copy();

            engine.frame(oldFrame);
            index = forkIndex;
        } else {
            // terminate the simulation
            index = backing.size();
        }
    }

    @Override
    public void advance() {
        while (visited.contains(index)) {
            // set the frame to the last frame
            engine.frame(frames.get(index));
            index++;
        }
        // unvisited index found
        // see if we need to fork

        frames.put(index, engine.frame().copy());

        CodeElement element = element();
        if (element instanceof ImmediateJumpInstruction imm) {
            // no fork needed, instantly jump to the target
            index = backing.indexOf(imm.target());
            return;
        } else if (element instanceof ConditionalJumpInstruction cond) {
            // fork
            int pos1 = index + 1;
            int pos2 = backing.indexOf(cond.target());

            // fork to pos1
            forkQueue.push(pos1);

            // jump to pos2
            index = pos2;
            return;
        } else if (element instanceof SimpleInstruction sim) {
            switch (sim.opcode()) {
                case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN -> {
                    visited.add(index);
                    forkOrExit();

                    return;
                }
                case ATHROW -> {
                    visited.add(index);
                    TryCatchBlock exceptionHandler = getExceptionHandler();
                    if(exceptionHandler == null) {
                        forkOrExit();
                    } else {
                        // load frame from beginning of exception handler
                        ClassType exType = engine.frame().peek();
                        Frame beginFrame = frames.get(backing.indexOf(exceptionHandler.start()));
                        Frame handlerFrame = new Frame(new ArrayDeque<>(), beginFrame.locals());
                        // TODO: merge exception types on re-visit
                        handlerFrame.push(exType);
                        index = backing.indexOf(exceptionHandler.handler());
                    }
                }
            }
        }

        visited.add(index++);
    }

    @Override
    public void set(Label label) {
        index = backing.indexOf(label);
    }
}
