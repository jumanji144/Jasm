package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class Instructions<V extends ASTInstructionVisitor> {

	private final Map<String, Instruction<V>> instructions = new HashMap<>();
	protected BiConsumer<ASTInstruction, V> defaultTranslator;

	public Instructions() {
		registerInstructions();
	}

	protected static Operand[] ops(Operands... operands) {
		Operand[] ops = new Operand[operands.length];
		for (int i = 0; i < operands.length; i++) {
			ops[i] = operands[i].getOperand();
		}
		return ops;
	}

	protected abstract void registerInstructions();

	public void register(String name, Operand[] operands, BiConsumer<ASTInstruction, V> translator) {
		instructions.put(name, new Instruction<>(operands, translator));
	}

	public void register(String name, BiConsumer<ASTInstruction, V> translator) {
		register(name, new Operand[0], translator);
	}

	public void register(String name) {
		register(name, new Operand[0], (instruction, visitor) -> {
		});
	}

	public void register(String... names) {
		for (String name : names) {
			register(name);
		}
	}

	public @Nullable Instruction<V> get(String name) {
		return instructions.get(name);
	}

}
