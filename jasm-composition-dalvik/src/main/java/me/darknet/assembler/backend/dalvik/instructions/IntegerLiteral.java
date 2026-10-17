package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandValue;

/**
 * Integer literal operand of an arithmetic instruction.
 * <p>
 * The value is kept exact and carries no encoding width. The emitter chooses the encoding that holds it once the
 * registers of the instruction are known.
 *
 * @param value
 * 		Literal value, within the range of an {@code int}.
 */
public record IntegerLiteral(int value) implements OperandValue {}
