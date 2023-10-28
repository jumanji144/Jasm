package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.simulation.ExecutionEngine;
import me.darknet.assembler.compile.analysis.FrameState;

/**
 * An extension of blw's execution engine which also outlines frame state tracking.
 */
public interface AnalysisEngine extends ExecutionEngine, FrameState {}
