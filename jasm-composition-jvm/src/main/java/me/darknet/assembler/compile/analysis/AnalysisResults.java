package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.CodeElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.frame.Frame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NavigableMap;

/**
 * Stack analysis results for a single method.
 */
public interface AnalysisResults {
    /**
     * @param index
     *              Key.
     *
     * @return Frame at index, or {@code null} if not present.
     */
    @Nullable
    default Frame getFrame(int index) {
        return frames().get(index);
    }

    /**
     * Map of instruction offsets to method stack frames. Keys are equal to the
     * indices of items within the {@link ASTCode#instructions()}.
     *
     * @return Navigable map of instruction offsets to frame information.
     */
    @NotNull
    NavigableMap<Integer, Frame> frames();

    /**
     * Map of instruction offsets to method stack frames. Keys are equal to the
     * indices of items within the {@link ASTCode#instructions()}. Keys will always
     * align with {@code return} and {@code athrow} instructions.
     *
     * @return Navigable map of terminal instruction offsets to frame information.
     */
    @NotNull
    NavigableMap<Integer, Frame> terminalFrames();

    /**
     * @return The exception thrown when handling method flow analysis. Will be
     *         {@code null} if analysis completed without problems.
     */
    @Nullable
    AnalysisException getAnalysisFailure();

    /**
     * @param analysisFailure
     *                        The exception thrown when handling method flow
     *                        analysis.
     */
    void setAnalysisFailure(@Nullable AnalysisException analysisFailure);

    /**
     * Records the relationship of AST --> Element for the given pair.
     *
     * @param instruction
     *         AST instruction.
     * @param element
     *         Generated code element.
     *
     * @see #getAstToCodeMap() AST --> Element mapping
     * @see #getAnalysisFailure() Element --> AST mapping
     */
    void recordInstructionMapping(@NotNull ASTInstruction instruction, @NotNull CodeElement element);

    /**
     * @return Map of AST instructions/labels to the generated code elements.
     */
    @NotNull Map<ASTInstruction, CodeElement> getAstToCodeMap();

    /**
     * @return Map of the generated code elements to their source AST instructions/labels.
     */
    @NotNull Map<CodeElement, ASTInstruction> getCodeToAstMap();
}
