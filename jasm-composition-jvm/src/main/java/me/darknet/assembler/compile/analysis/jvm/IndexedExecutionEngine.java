package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.simulation.ExecutionEngine;

/**
 * An engine which is intended for use in
 * {@link IndexedStraightforwardSimulation}. The engine is not for proper
 * stack/local analysis, but for just visiting each {@link CodeElement} in a
 * method in a linear fashion.
 */
public interface IndexedExecutionEngine extends ExecutionEngine {
    /**
     * Marks the index as the current target for execution.
     *
     * @param index
     *              Index to set.
     */
    void index(int index);
}
