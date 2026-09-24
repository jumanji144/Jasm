package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Common base class for instruction registries.
 *
 * @param <V>
 * 		Type of visitor that instructions in this set will be translated to.
 */
public abstract class Instructions<V extends ASTInstructionVisitor> {
	private Map<String, Instruction<V>> instructions = new HashMap<>();
	private boolean registrationOpen = true;
	protected BiConsumer<ASTInstruction, V> defaultTranslator;

	protected Instructions() {
		this(true);
	}

	/**
	 * @param registerInstructions
	 * 		whether to invoke and freeze the registry hook during construction.
	 *        When false, the subclass must invoke {@link #registerInstructions()} and then {@link #freeze()}
	 *        after its fields have been initialized.
	 */
	protected Instructions(boolean registerInstructions) {
		if (registerInstructions) {
			registerInstructions();
			freeze();
		}
	}

	protected static Operand[] ops(Operands... operands) {
		Operand[] ops = new Operand[operands.length];
		for (int i = 0; i < operands.length; i++)
			ops[i] = operands[i].getOperand();
		return ops;
	}

	/**
	 * Register all instructions needed for this instruction set.
	 */
	protected abstract void registerInstructions();

	/**
	 * Registers a semantic instruction with immutable metadata.
	 *
	 * @throws UnsupportedOperationException
	 * 		if this registry has been frozen.
	 * @throws IllegalArgumentException
	 * 		if another instruction already owns {@code name}.
	 */
	protected final void register(
			String name,
			Operand[] operands,
			BiConsumer<SemanticInstruction, V> translator,
			InstructionMetadata metadata) {
		ensureCanRegister(name);
		instructions.put(name, new Instruction<>(name, operands, translator, metadata));
	}

	/**
	 * Legacy AST-translator registration retained until the old core target registries are removed.
	 *
	 * @throws UnsupportedOperationException
	 * 		if this registry has been frozen.
	 * @throws IllegalArgumentException
	 * 		if another instruction already owns {@code name}.
	 */
	public final void register(String name, Operand[] operands, BiConsumer<ASTInstruction, V> translator) {
		ensureCanRegister(name);
		InstructionMetadata metadata = InstructionMetadata.of(
				EnumSet.noneOf(InstructionTrait.class), List.of(), null, name, null, null);
		instructions.put(name, Instruction.legacy(name, operands, translator, metadata));
	}

	/**
	 * Registers a legacy AST-translated instruction that takes no operands.
	 */
	public final void register(String name, BiConsumer<ASTInstruction, V> translator) {
		register(name, new Operand[0], translator);
	}

	/**
	 * Registers a legacy instruction that takes no operands and has no translation.
	 */
	public final void register(String name) {
		register(name, new Operand[0], (instruction, visitor) -> {});
	}

	/**
	 * Registers several legacy instructions that take no operands and have no translation.
	 */
	public final void register(String... names) {
		for (String name : names)
			register(name);
	}

	private void ensureCanRegister(String name) {
		if (!registrationOpen)
			throw new UnsupportedOperationException("Registry is immutable");
		Objects.requireNonNull(name, "name");
		if (instructions.containsKey(name))
			throw new IllegalArgumentException("Duplicate instruction: " + name);
	}

	/**
	 * Freezes the registry after all instruction definitions have been registered.
	 */
	protected final void freeze() {
		if (!registrationOpen)
			return;
		instructions = Map.copyOf(instructions);
		registrationOpen = false;
	}

	/**
	 * Creates metadata for a target-backed instruction declaration.
	 */
	protected final InstructionMetadata metadata(
			EnumSet<InstructionTrait> traits,
			List<OperandRole> roles,
			@Nullable SwitchShape switchShape,
			String canonicalName,
			@Nullable InstructionLowering lowering) {
		return metadata(traits, roles, switchShape, canonicalName, lowering, null);
	}

	/**
	 * Creates metadata for a target-backed instruction unavailable to the declaring target.
	 */
	protected final InstructionMetadata metadata(
			EnumSet<InstructionTrait> traits,
			List<OperandRole> roles,
			@Nullable SwitchShape switchShape,
			String canonicalName,
			@Nullable InstructionLowering lowering,
			@Nullable String unavailableReason) {
		return InstructionMetadata.of(traits, roles, switchShape, canonicalName, lowering, unavailableReason);
	}

	/**
	 * Creates metadata for a source-only instruction declaration.
	 */
	protected final InstructionMetadata sourceMetadata(
			EnumSet<InstructionTrait> traits,
			List<OperandRole> roles,
			@Nullable SwitchShape switchShape,
			String canonicalName) {
		return InstructionMetadata.of(traits, roles, switchShape, canonicalName, null, null);
	}

	/**
	 * @return Handler for the instruction, or {@code null} if no handler is registered.
	 */
	public @Nullable Instruction<V> get(String name) {
		return instructions.get(name);
	}

	/**
	 * @return Set of instruction names that have registered handlers.
	 */
	public @NotNull Set<String> getInstructionNames() {
		return Set.copyOf(instructions.keySet());
	}

	/**
	 * @return Every registered source mnemonic, which is exactly the set a user may write.
	 */
	public @NotNull Set<String> getSourceKeywords() {
		return getInstructionNames();
	}

	/**
	 * @return Canonical mnemonics exposed by the registered definitions.
	 */
	public @NotNull Set<String> getCanonicalKeywords() {
		return instructions.values().stream()
				.map(Instruction::canonicalName)
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * @param canonicalName
	 * 		canonical mnemonic to enumerate.
	 * @return Registered mnemonics declaring {@code canonicalName} as canonical, excluding the canonical spelling.
	 */
	public @NotNull List<String> aliasesOf(@NotNull String canonicalName) {
		Objects.requireNonNull(canonicalName, "canonicalName");
		return instructions.values().stream()
				.filter(instruction -> instruction.canonicalName().equals(canonicalName))
				.map(Instruction::name)
				.filter(name -> !name.equals(canonicalName))
				.sorted()
				.toList();
	}
}
