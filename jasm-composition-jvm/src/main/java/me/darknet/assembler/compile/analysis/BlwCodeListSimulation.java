package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.simulation.ExecutionEngine;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;

import java.util.List;

public class BlwCodeListSimulation implements Simulation<ExecutionEngine, List<CodeElement>> {
    @Override
    public void execute(ExecutionEngine engine, List<CodeElement> method) {
        for (CodeElement element : method) {
            if(element instanceof Label l) {
                engine.label(l);
            } else {
                ExecutionEngines.execute(engine, (Instruction) element);
            }
        }
    }
}
