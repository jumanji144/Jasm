package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable analysis run state for a single method.
 */
public class AnalysisSession<F extends Frame> {
	private final MethodAnalysisResult result;
	private @Nullable F frame;
	private int frameIndex;

	public AnalysisSession(@NotNull MethodAnalysisResult result) {
		this.result = result;
	}

	public @NotNull MethodAnalysisResult result() {
		return result;
	}

	public @Nullable F frame() {
		return frame;
	}

	public int frameIndex() {
		return frameIndex;
	}

	public void setActiveFrame(int frameIndex, @NotNull F frame) {
		this.frameIndex = frameIndex;
		this.frame = frame;
	}

	@SuppressWarnings("unchecked")
	public @Nullable F getFrame(int index) {
		return (F) result.frames().get(index);
	}

	public void putFrame(int index, @NotNull F frame) {
		result.frames().put(index, frame);
	}

	@SuppressWarnings("unchecked")
	public boolean putAndMergeFrame(@NotNull InheritanceChecker checker, int index, @NotNull F frame) throws FrameMergeException {
		F old = getFrame(index);
		if (old == null) {
			putFrame(index, frame);
			return true;
		}

		F merged = (F) old.copy();
		boolean changed = merged.merge(checker, frame);
		putFrame(index, merged);
		return changed;
	}

	public void markTerminal(int index, @NotNull F frame) {
		result.terminalFrames().put(index, frame);
	}
}
