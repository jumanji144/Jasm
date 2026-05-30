package me.darknet.assembler.compile.analysis.jvm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

/**
 * An engine which is intended for use in {@link IndexedStraightforwardSimulation}.
 * The engine is not for proper stack/local analysis, but for just visiting each ASM node in a
 * method in a linear fashion.
 */
public interface IndexedExecutionEngine {
	/**
	 * Marks the index as the current target for execution.
	 *
	 * @param index
	 * 		Index to set.
	 */
	void index(int index);

	/**
	 * Marks the label as the current target for execution.
	 *
	 * @param label
	 * 		Label to set.
	 */
	void label(LabelNode label);

	/**
	 * Executes the instruction at the current index.
	 *
	 * @param instruction
	 * 		Instruction to execute.
	 */
	void execute(AbstractInsnNode instruction);
}
