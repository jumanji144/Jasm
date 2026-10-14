package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable analysis results for a single method.
 */
public final class DalvikAnalysisResults {
	private final MethodReference method;
	private final List<Instruction> codeInstructions;
	private final Map<Integer, DalvikRegisterState> instructionStates;
	private final Map<Integer, TerminalState> terminalStates;
	private final Map<Instruction, ASTInstruction> instructionToSource;
	private final Map<Instruction, Integer> instructionIndices;
	private final List<DalvikAnalysisFailure> failures;

	public DalvikAnalysisResults(@NotNull MethodReference method,
	                             @NotNull List<Instruction> codeInstructions,
	                             @NotNull Map<Integer, DalvikRegisterState> instructionStates,
	                             @NotNull Map<Integer, TerminalState> terminalStates,
	                             @NotNull Map<Instruction, ASTInstruction> instructionToSource,
	                             @NotNull List<DalvikAnalysisFailure> failures) {
		this.method = Objects.requireNonNull(method, "method");
		this.codeInstructions = List.copyOf(codeInstructions);

		Map<Integer, DalvikRegisterState> orderedInstructionStates = new TreeMap<>(instructionStates);
		Map<Integer, TerminalState> orderedTerminalStates = new TreeMap<>(terminalStates);
		Map<Instruction, ASTInstruction> sourceCopy = new IdentityHashMap<>(instructionToSource);
		Map<Instruction, Integer> indices = new IdentityHashMap<>(codeInstructions.size());
		for (int index = 0; index < codeInstructions.size(); index++)
			indices.put(codeInstructions.get(index), index);

		this.instructionStates = Collections.unmodifiableMap(orderedInstructionStates);
		this.terminalStates = Collections.unmodifiableMap(orderedTerminalStates);
		this.instructionToSource = Collections.unmodifiableMap(sourceCopy);
		this.instructionIndices = Collections.unmodifiableMap(indices);
		this.failures = List.copyOf(failures);

	}

	/**
	 * @return Reference to the analyzed method.
	 */
	public @NotNull MethodReference method() {
		return method;
	}

	/**
	 * @return A snapshot of every entry in the dex code sequence, including labels.
	 */
	public @NotNull List<Instruction> getCodeInstructions() {
		return codeInstructions;
	}

	/**
	 * @return Reachable executable instructions mapped to their state before execution, ordered by code index.
	 */
	public @NotNull Map<Integer, DalvikRegisterState> getInstructionStates() {
		return instructionStates;
	}

	/**
	 * @return Terminal states ordered by the terminal instruction's code index.
	 */
	public @NotNull Map<Integer, TerminalState> getTerminalStates() {
		return terminalStates;
	}

	/**
	 * @return An unmodifiable identity-keyed snapshot of executable source mappings.
	 */
	public @NotNull Map<Instruction, ASTInstruction> getInstructionToSource() {
		return instructionToSource;
	}

	/**
	 * @return List of analysis failures, if any, in the order they were recorded.
	 */
	public @NotNull List<DalvikAnalysisFailure> getFailures() {
		return failures;
	}

	/**
	 * @param instruction
	 * 		Instruction to locate.
	 *
	 * @return Position in {@link #getCodeInstructions()}, or {@code null} if absent.
	 */
	public @Nullable Integer getInstructionIndex(@Nullable Instruction instruction) {
		return instructionIndices.get(instruction);
	}

	/**
	 * @param instruction
	 * 		Executable dex instruction to look up.
	 *
	 * @return Original AST instruction, or {@code null} if unmapped.
	 */
	public @Nullable ASTInstruction getSource(@Nullable Instruction instruction) {
		return instructionToSource.get(instruction);
	}

	/**
	 * @param instructionIndex
	 * 		Position in the dex code sequence.
	 *
	 * @return State immediately before a reachable executable instruction, or {@code null} when absent.
	 */
	public @Nullable DalvikRegisterState getStateBefore(int instructionIndex) {
		return instructionStates.get(instructionIndex);
	}

	/**
	 * @param instruction
	 * 		Executable dex instruction.
	 *
	 * @return State immediately before this reachable instruction, or {@code null} when absent.
	 */
	public @Nullable DalvikRegisterState getStateBefore(@Nullable Instruction instruction) {
		Integer index = getInstructionIndex(instruction);
		return index == null ? null : getStateBefore(index);
	}

	/**
	 * Kind of method exit represented by a terminal state.
	 */
	public enum TerminalKind {
		RETURN,
		THROW
	}

	/**
	 * State immediately before a return or throw instruction.
	 *
	 * @param kind
	 * 		Terminal instruction kind.
	 * @param registerState
	 * 		Register state before the terminal instruction.
	 * @param value
	 * 		Returned or thrown value, absent only for {@code return-void}.
	 */
	public record TerminalState(@NotNull TerminalKind kind,
	                            @NotNull DalvikRegisterState registerState,
	                            @Nullable Value value) {}
}
