package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandValue;

/**
 * Integer literal operand carrying the encoding width its instruction accepts.
 *
 * @param value
 * 		Literal value, already narrowed to the signed range {@code bits} allows.
 * @param bits
 * 		Declared encoding width in bits, either {@code 8} or {@code 16}.
 */
public record SignedLiteral(int value, int bits) implements OperandValue {}
