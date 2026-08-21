package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.compile.analysis.frame.Frame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Stack analysis results for a single method.
 */
public interface AnalysisResults {
	/**
	 * @param index
	 * 		Key.
	 *
	 * @return Frame at index, or {@code null} if not present.
	 */
	@Nullable
	default Frame getFrame(int index) {
		return frames().get(index);
	}

	/**
	 * @param instruction
	 * 		The generated instruction node to locate.
	 *
	 * @return The index of the given node within the analyzed method body, or
	 * {@code null} if it is not present (for example, a label that was
	 * dropped from the final class).
	 */
	@Nullable
	Integer getInstructionIndex(@NotNull AbstractInsnNode instruction);

	/**
	 * @param instruction
	 * 		The generated instruction node to look up.
	 *
	 * @return The frame observed immediately before the given instruction (the nearest
	 * stored frame at or before its index), or {@code null} if there is none.
	 */
	@Nullable
	Frame getFrame(@NotNull AbstractInsnNode instruction);

	/**
	 * @param instruction
	 * 		The AST instruction to look up.
	 *
	 * @return The frame observed before the given AST instruction's generated node,
	 * or {@code null} if there is none. Labels and line markers do not have
	 * frames and return {@code null}.
	 */
	@Nullable
	Frame getFrame(@NotNull ASTInstruction instruction);

	/**
	 * Map of instruction offsets to method stack frames. Keys are indices into the
	 * analyzed (frame-computed) method body; the frame at key {@code i} is the state
	 * observed before executing instruction {@code i}.
	 *
	 * @return Navigable map of instruction offsets to frame information.
	 */
	@NotNull
	NavigableMap<Integer, Frame> frames();

	/**
	 * Map of instruction offsets to method stack frames. Keys are indices into the
	 * analyzed (frame-computed) method body and always align with {@code return} and
	 * {@code athrow} instructions.
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
	 * {@code null} if analysis completed without problems.
	 */
	@Nullable
	AnalysisException getAnalysisFailure();

	/**
	 * Combined view of executable instructions, labels, and line markers mapped to
	 * their generated nodes. Returns a snapshot, so mutating it does not affect this result.
	 *
	 * @return Map of AST code items to their generated instruction nodes.
	 */
	@NotNull Map<ASTInstruction, AbstractInsnNode> getAstToInstructionMap();

	/**
	 * Combined reverse view of the {@link #getAstToInstructionMap()} mapping.
	 *
	 * @return Map of generated instruction nodes to their source AST code items.
	 */
	@NotNull Map<AbstractInsnNode, ASTInstruction> getInstructionToAstMap();

	/**
	 * Maps executable AST instructions (no line #/labels) to the generated instruction nodes from the
	 * final (frame-computed) method body. This is the live map used for analysis
	 * correlation, keyed by the nodes the analyzer actually executed.
	 *
	 * @return Map of executable AST instructions to their generated instruction nodes.
	 */
	@NotNull Map<ASTInstruction, AbstractInsnNode> getAstToExecutableInstructionMap();

	/**
	 * Reverse view of the {@link #getAstToExecutableInstructionMap()} mapping.
	 *
	 * @return Map of generated instruction nodes to their source executable AST instructions.
	 */
	@NotNull Map<AbstractInsnNode, ASTInstruction> getExecutableInstructionToAstMap();

	/**
	 * Maps source labels to the {@link LabelNode}s emitted for them. These nodes are
	 * recorded at emission time since unreferenced labels can be dropped from the
	 * final class body.
	 *
	 * @return Map of source labels to their emitted label nodes.
	 */
	@NotNull Map<ASTLabel, LabelNode> getAstToLabelMap();

	/**
	 * @return Map of emitted label nodes to their source labels.
	 */
	@NotNull Map<LabelNode, ASTLabel> getLabelToAstMap();

	/**
	 * Maps {@code line <number>} AST instructions to the {@link LineNumberNode}s
	 * emitted for them.
	 *
	 * @return Map of line-number AST instructions to their emitted line-number nodes.
	 */
	@NotNull Map<ASTInstruction, LineNumberNode> getAstToLineNumberMap();

	/**
	 * @return Map of emitted line-number nodes to their source {@code line} AST instructions.
	 */
	@NotNull Map<LineNumberNode, ASTInstruction> getLineNumberToAstMap();
}
