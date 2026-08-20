package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.frame.Frame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;
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
     * @return Immutable scoped local-variable states.
     */
    @NotNull
    List<LocalVariableState> localVariableStates();

    /**
     * @return The exception thrown when handling method flow analysis. Will be
     *         {@code null} if analysis completed without problems.
     */
    @Nullable
    AnalysisException getAnalysisFailure();

    /**
     * @return Map of AST instructions/labels to the generated instruction nodes.
     */
    @NotNull Map<ASTInstruction, AbstractInsnNode> getAstToInstructionMap();

    /**
     * @return Map of the generated instruction nodes to their source AST instructions/labels.
     */
    @NotNull Map<AbstractInsnNode, ASTInstruction> getInstructionToAstMap();
}
