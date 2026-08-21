package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.compile.analysis.frame.Frame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Mutable analysis result for a single method.
 */
public class MethodAnalysisResult implements AnalysisResults {
	private final NavigableMap<Integer, Frame> frames = new TreeMap<>();
	private final NavigableMap<Integer, Frame> terminalFrames = new TreeMap<>();
	private final Map<AbstractInsnNode, Integer> instructionToIndex = new IdentityHashMap<>();
	private final List<ASTInstruction> executableAstInstructions = new ArrayList<>();
	private final Map<ASTInstruction, AbstractInsnNode> astToExecutableInstruction = new IdentityHashMap<>();
	private final Map<AbstractInsnNode, ASTInstruction> executableInstructionToAst = new IdentityHashMap<>();
	private final Map<ASTLabel, LabelNode> astToLabel = new IdentityHashMap<>();
	private final Map<LabelNode, ASTLabel> labelToAst = new IdentityHashMap<>();
	private final Map<ASTInstruction, LineNumberNode> astToLineNumber = new IdentityHashMap<>();
	private final Map<LineNumberNode, ASTInstruction> lineNumberToAst = new IdentityHashMap<>();
	private List<LocalVariableState> localVariableStates = List.of();
	private AnalysisException analysisFailure;

	@Override
	public @NotNull NavigableMap<Integer, Frame> frames() {
		return frames;
	}

	@Override
	public @NotNull NavigableMap<Integer, Frame> terminalFrames() {
		return terminalFrames;
	}

	@Override
	public @NotNull List<LocalVariableState> localVariableStates() {
		return localVariableStates;
	}

	/**
	 * @param states
	 * 		Scoped source-level states to store.
	 */
	public void setLocalVariableStates(@NotNull List<LocalVariableState> states) {
		localVariableStates = List.copyOf(states);
	}

	@Override
	public @Nullable AnalysisException getAnalysisFailure() {
		return analysisFailure;
	}

	public void setAnalysisFailure(@Nullable AnalysisException analysisFailure) {
		this.analysisFailure = analysisFailure;
	}

	public void resetAnalysisState() {
		frames.clear();
		terminalFrames.clear();
		instructionToIndex.clear();
		astToExecutableInstruction.clear();
		executableInstructionToAst.clear();
		localVariableStates = List.of();
		analysisFailure = null;
	}

	public void recordExecutableInstruction(@NotNull ASTInstruction instruction) {
		if (!executableAstInstructions.contains(instruction))
			executableAstInstructions.add(instruction);
	}

	public void recordLabelMapping(@NotNull ASTLabel label, @NotNull LabelNode node) {
		astToLabel.put(label, node);
		labelToAst.put(node, label);
	}

	public void recordLineNumberMapping(@NotNull ASTInstruction instruction, @NotNull LineNumberNode node) {
		astToLineNumber.put(instruction, node);
		lineNumberToAst.put(node, instruction);
	}

	public void recordExecutableInstructionMapping(@Nullable ASTInstruction instruction, @NotNull AbstractInsnNode node) {
		executableInstructionToAst.put(node, instruction);
		if (instruction != null)
			astToExecutableInstruction.put(instruction, node);
	}

	public void recordInstructionIndex(@NotNull AbstractInsnNode instruction, int index) {
		instructionToIndex.put(instruction, index);
	}

	@Override
	public @Nullable Integer getInstructionIndex(@NotNull AbstractInsnNode instruction) {
		return instructionToIndex.get(instruction);
	}

	@Override
	public @Nullable Frame getFrame(@NotNull AbstractInsnNode instruction) {
		Integer index = instructionToIndex.get(instruction);
		if (index == null)
			return null;

		// Frames are stored at flow points (entry, branch targets, and after each
		// executable), so the state before this instruction is the nearest stored
		// frame at or before its index.
		Map.Entry<Integer, Frame> entry = frames.floorEntry(index);
		return entry == null ? null : entry.getValue();
	}

	@Override
	public @Nullable Frame getFrame(@NotNull ASTInstruction instruction) {
		AbstractInsnNode node = astToExecutableInstruction.get(instruction);
		if (node == null)
			node = astToLabel.get(instruction);
		if (node == null)
			node = astToLineNumber.get(instruction);
		return node == null ? null : getFrame(node);
	}

	public @NotNull List<ASTInstruction> getExecutableAstInstructions() {
		return executableAstInstructions;
	}

	@Override
	public @NotNull Map<ASTLabel, LabelNode> getAstToLabelMap() {
		return astToLabel;
	}

	@Override
	public @NotNull Map<LabelNode, ASTLabel> getLabelToAstMap() {
		return labelToAst;
	}

	@Override
	public @NotNull Map<ASTInstruction, LineNumberNode> getAstToLineNumberMap() {
		return astToLineNumber;
	}

	@Override
	public @NotNull Map<LineNumberNode, ASTInstruction> getLineNumberToAstMap() {
		return lineNumberToAst;
	}

	@Override
	public @NotNull Map<ASTInstruction, AbstractInsnNode> getAstToInstructionMap() {
		Map<ASTInstruction, AbstractInsnNode> result = new IdentityHashMap<>(astToExecutableInstruction);
		result.putAll(astToLabel);
		result.putAll(astToLineNumber);
		return result;
	}

	@Override
	public @NotNull Map<AbstractInsnNode, ASTInstruction> getInstructionToAstMap() {
		Map<AbstractInsnNode, ASTInstruction> result = new IdentityHashMap<>(executableInstructionToAst);
		result.putAll(labelToAst);
		result.putAll(lineNumberToAst);
		return result;
	}

	@Override
	public @NotNull Map<ASTInstruction, AbstractInsnNode> getAstToExecutableInstructionMap() {
		return astToExecutableInstruction;
	}

	@Override
	public @NotNull Map<AbstractInsnNode, ASTInstruction> getExecutableInstructionToAstMap() {
		return executableInstructionToAst;
	}
}
