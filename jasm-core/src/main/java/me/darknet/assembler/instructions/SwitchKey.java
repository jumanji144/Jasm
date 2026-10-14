package me.darknet.assembler.instructions;

/**
 * Integer switch case key parsed from a source literal.
 *
 * @param value
 * 		Parsed key. Radix prefixes and a leading sign are already applied, so the value is the
 * 		same bit pattern the backend emits.
 */
public record SwitchKey(int value) implements OperandValue {}
