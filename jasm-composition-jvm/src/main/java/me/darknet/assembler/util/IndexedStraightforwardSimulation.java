package me.darknet.assembler.util;

import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.code.Code;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;

import java.util.List;
import java.util.Objects;

public class IndexedStraightforwardSimulation implements Simulation<IndexedExecutionEngine, Method> {
    @Override
    public void execute(IndexedExecutionEngine engine, Method method) {
        Code code = Objects.requireNonNull(method.code(), "method.code()");
        List<CodeElement> elements = code.elements();
        for (int i = 0; i < elements.size(); i++) {
            engine.index(i);
            CodeElement element = elements.get(i);
            if (element instanceof Label) {
                engine.label((Label) element);
            } else {
                ExecutionEngines.execute(engine, (Instruction) element);
            }
        }
    }
}
