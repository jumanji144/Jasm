package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableMap;

/**
 * Stack analysis results for a single method.
 */
public interface AnalysisResults<L extends Local, S extends StackEntry, F extends AbstractFrame<L, S>>
		extends FrameState<L, S, F> {
	/**
	 * Map of instruction offsets to method stack frames.
	 * Keys are equal to the indices of items within the {@link ASTCode#instructions()}.
	 *
	 * @return Navigable map of instruction offsets to frame information.
	 */
	@NotNull
	NavigableMap<Integer, F> frames();

	/**
	 * @return The exception thrown when handling method flow analysis.
	 * Will be {@code null} if analysis completed without problems.
	 */
	@Nullable
	AnalysisException getAnalysisFailure();

	/**
	 * @param analysisFailure
	 * 		The exception thrown when handling method flow analysis.
	 */
	void setAnalysisFailure(@Nullable AnalysisException analysisFailure);
}
