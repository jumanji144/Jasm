package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.ast.primitive.ASTNumber;
import org.jetbrains.annotations.NotNull;

/**
 * Interpretation of Dalvik {@code fill-array-data} numeric literals.
 */
public final class DalvikArrayLiterals {
	private DalvikArrayLiterals() {}

	/**
	 * @param number
	 * 		Literal to inspect.
	 *
	 * @return {@code true} when the literal denotes a floating-point value rather than an integer.
	 */
	public static boolean isFloating(@NotNull ASTNumber number) {
		String literal = number.content().toLowerCase();

		// Radix-prefixed literals are integer literals even though the parser marks them as having a
		// fractional form; only a genuine decimal point or exponent makes a value floating point.
		if (literal.startsWith("0x") || literal.startsWith("-0x") || literal.startsWith("0b") || literal.startsWith("-0b"))
			return false;

		return number.isFloatingPoint();
	}

	/**
	 * @param number
	 * 		Literal to parse.
	 *
	 * @return Signed integral value of the literal.
	 *
	 * @throws NumberFormatException
	 * 		If the literal carries no digits that can be parsed in its radix.
	 */
	public static long toIntegral(@NotNull ASTNumber number) {
		String literal = number.content().toLowerCase();
		boolean negative = literal.startsWith("-");
		if (negative)
			literal = literal.substring(1);

		int radix = 10;
		if (literal.startsWith("0x")) {
			radix = 16;
			literal = literal.substring(2);
		} else if (literal.startsWith("0b")) {
			radix = 2;
			literal = literal.substring(2);
		}

		// The long suffix is a source marker only; the value itself is what gets encoded.
		if (literal.endsWith("l"))
			literal = literal.substring(0, literal.length() - 1);

		if (literal.isEmpty())
			throw new NumberFormatException("Expected integral literal");

		if (negative)
			return -Long.parseLong(literal, radix);

		// A radix-prefixed literal is unsigned so a full-width pattern such as 0xFFFFFFFFFFFFFFFF
		// stays expressible without a sign.
		if (radix != 10)
			return Long.parseUnsignedLong(literal, radix);
		return Long.parseLong(literal, radix);
	}
}
