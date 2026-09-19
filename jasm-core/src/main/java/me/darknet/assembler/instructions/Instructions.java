package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.instructions.dalvik.DalvikInstructions;
import me.darknet.assembler.instructions.jvm.JvmInstructions;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Common base class for instruction sets.
 *
 * @param <V>
 * 		Type of visitor that instructions in this set will be translated to.
 *
 * @see JvmInstructions
 * @see DalvikInstructions
 */
public abstract class Instructions<V extends ASTInstructionVisitor> {
	private final Map<String, Instruction<V>> instructions = new HashMap<>();
	protected BiConsumer<ASTInstruction, V> defaultTranslator;

	protected Instructions() {
		registerInstructions();
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
	 * Register a handler for an instruction.
	 *
	 * @param name
	 * 		Name of the instruction.
	 * @param operands
	 * 		Operands that the instruction takes.
	 * @param translator
	 * 		Translator that will translate the instruction to the visitor.
	 */
	public void register(String name, Operand[] operands, BiConsumer<ASTInstruction, V> translator) {
		instructions.put(name, new Instruction<>(operands, translator));
	}

	/**
	 * Register a handler for an instruction that takes no operands.
	 *
	 * @param name
	 * 		Name of the instruction.
	 * @param translator
	 * 		Translator that will translate the instruction to the visitor.
	 */
	public void register(String name, BiConsumer<ASTInstruction, V> translator) {
		register(name, new Operand[0], translator);
	}

	/**
	 * Register a handler for an instruction that takes no operands and has no translation.
	 *
	 * @param name
	 * 		Name of the instruction.
	 */
	public void register(String name) {
		register(name, new Operand[0], (instruction, visitor) -> {});
	}

	/**
	 * Register multiple instructions that take no operands and have no translation.
	 *
	 * @param names
	 * 		Names of the instructions.
	 */
	public void register(String... names) {
		for (String name : names)
			register(name);

	}

	/**
	 * @param name
	 * 		Name of instruction to get the handler for.
	 *
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
}
