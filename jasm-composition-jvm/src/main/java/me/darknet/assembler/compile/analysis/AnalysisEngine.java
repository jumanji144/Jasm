package me.darknet.assembler.compile.analysis;

/**
 * Outline of analysis computations.
 */
public interface AnalysisEngine<L extends Local, S extends StackEntry, F extends AbstractFrame<L, S>>
		extends FrameState<L, S, F> {
}
