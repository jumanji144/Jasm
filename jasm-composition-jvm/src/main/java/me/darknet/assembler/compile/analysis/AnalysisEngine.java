package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.simulation.ExecutionEngine;

public interface AnalysisEngine extends ExecutionEngine {

    Frame frame();

    void frame(Frame frame);

}
