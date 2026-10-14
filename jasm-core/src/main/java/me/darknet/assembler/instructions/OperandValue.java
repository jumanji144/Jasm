package me.darknet.assembler.instructions;

/**
 * Typed semantic value produced for one source operand.
 * <p>
 * Target modules declare their own implementations next to the operand schema that produces them.
 * This keeps the core module free of any target-specific types.
 */
public interface OperandValue {}
