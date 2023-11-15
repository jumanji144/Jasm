package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTCode;
import org.jetbrains.annotations.NotNull;

import java.util.NavigableMap;

/**
 * Stack analysis results for a single method.
 */
public interface AnalysisResults extends FrameState {
	/**
	 * Map of instruction offsets to method stack frames.
	 * Keys are equal to the indices of items within the {@link ASTCode#instructions()}.
	 *
	 * @return Navigable map of instruction offsets to frame information.
	 */
	@NotNull
	NavigableMap<Integer, Frame> frames();
}
