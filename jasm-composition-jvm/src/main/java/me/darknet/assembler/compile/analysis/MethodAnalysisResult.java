package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.frame.Frame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;

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
	private final Map<ASTInstruction, AbstractInsnNode> astToInstruction = new IdentityHashMap<>();
	private final Map<AbstractInsnNode, ASTInstruction> instructionToAst = new IdentityHashMap<>();
	private final List<ASTInstruction> orderedAstInstructions = new ArrayList<>();
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
	public @Nullable AnalysisException getAnalysisFailure() {
		return analysisFailure;
	}

	public void setAnalysisFailure(@Nullable AnalysisException analysisFailure) {
		this.analysisFailure = analysisFailure;
	}

	public void resetAnalysisState() {
		frames.clear();
		terminalFrames.clear();
		astToInstruction.clear();
		instructionToAst.clear();
		analysisFailure = null;
	}

	public void recordOrderedInstruction(@NotNull ASTInstruction instruction) {
		orderedAstInstructions.add(instruction);
	}

	public @NotNull List<ASTInstruction> getOrderedAstInstructions() {
		return orderedAstInstructions;
	}

	public void recordInstructionMapping(@Nullable ASTInstruction instruction, @NotNull AbstractInsnNode node) {
		instructionToAst.put(node, instruction);
		if (instruction != null)
			astToInstruction.put(instruction, node);
	}

	@Override
	public @NotNull Map<ASTInstruction, AbstractInsnNode> getAstToInstructionMap() {
		return astToInstruction;
	}

	@Override
	public @NotNull Map<AbstractInsnNode, ASTInstruction> getInstructionToAstMap() {
		return instructionToAst;
	}
}
