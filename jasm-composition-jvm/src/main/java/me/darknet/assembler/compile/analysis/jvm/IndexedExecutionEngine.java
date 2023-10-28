package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.simulation.ExecutionEngine;

public interface IndexedExecutionEngine extends ExecutionEngine {

    void index(int index);

}
