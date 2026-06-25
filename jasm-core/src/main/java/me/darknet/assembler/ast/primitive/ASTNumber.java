package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

public class ASTNumber extends ASTValue {

	/**
	 * @param number
	 * 		Backing token for this numeric literal.
	 */
	public ASTNumber(Token number) {
		super(ElementType.NUMBER, number);
	}

	/**
	 * Parse this token into its runtime value.
	 *
	 * @return Parsed numeric value as the narrowest matching boxed type.
	 *
	 * @throws NumberFormatException
	 * 		When the token content is not a valid supported number literal.
	 */
	public Number number() {
		String value = normalizedContent();
		String lower = value.toLowerCase();
		if (lower.startsWith("nan")) {
			if (lower.endsWith("f"))
				return Float.NaN;
			return Double.NaN;
		} else if (lower.contains("infinity")) {
			if (lower.startsWith("-")) {
				if (lower.endsWith("f"))
					return Float.NEGATIVE_INFINITY;
				return Double.NEGATIVE_INFINITY;
			}
			if (lower.endsWith("f"))
				return Float.POSITIVE_INFINITY;
			return Double.POSITIVE_INFINITY;
		} else if (isHexFloatingPoint(lower) || isDecimalFloatingPoint(lower)) {
			if (lower.endsWith("f"))
				return Float.parseFloat(value.substring(0, value.length() - 1));
			if (lower.endsWith("d"))
				return Double.parseDouble(value.substring(0, value.length() - 1));
			return Double.parseDouble(value);
		} else if (hasRadixPrefix(lower, "0x")) {
			return parseInteger(value, 16, 2);
		} else if (hasRadixPrefix(lower, "0b")) {
			return parseInteger(value, 2, 2);
		} else {
			return parseInteger(value, 10, 0);
		}
	}

	/**
	 * @return {@code true} when this literal occupies a wide slot,
	 * meaning it resolves to a {@code long} or {@code double}.
	 */
	public boolean isWide() {
		String value = normalizedContent().toLowerCase();
		if (value.startsWith("nan") || value.contains("infinity"))
			return !value.endsWith("f");
		if (isHexFloatingPoint(value) || isDecimalFloatingPoint(value))
			return !value.endsWith("f");
		return value.endsWith("l");
	}

	/**
	 * @return Parsed value interpreted via {@link Number#intValue()}.
	 */
	public int asInt() {
		return number().intValue();
	}

	/**
	 * @return Parsed value interpreted via {@link Number#longValue()}.
	 */
	public long asLong() {
		return number().longValue();
	}

	/**
	 * @return Parsed value interpreted via {@link Number#floatValue()}.
	 */
	public float asFloat() {
		return number().floatValue();
	}

	/**
	 * @return Parsed value interpreted via {@link Number#doubleValue()}.
	 */
	public double asDouble() {
		return number().doubleValue();
	}

	/**
	 * @return {@code true} when this number is a {@code float} or {@code double}.
	 */
	public boolean isFloatingPoint() {
		String value = normalizedContent().toLowerCase();
		return value.startsWith("nan")
				|| value.contains("infinity")
				|| isHexFloatingPoint(value)
				|| isDecimalFloatingPoint(value);
	}

	/**
	 * @return {@code true} when this literal is {@code NaN}.
	 */
	public boolean isNaN() {
		String value = content().toLowerCase();
		return value.equals("nan") || value.equals("nand") || value.equals("nanf");
	}

	/**
	 * @return {@code true} when this literal is {@code Infinity}.
	 */
	public boolean isInfinity() {
		String value = content().toLowerCase();
		return value.equals("infinity") || value.equals("+infinity") || value.equals("-infinity") ||
				value.equals("infinityd") || value.equals("+infinityd") || value.equals("-infinityd") ||
				value.equals("infinityf") || value.equals("+infinityf") || value.equals("-infinityf");
	}

	/**
	 * @return Literal content without underscore separators.
	 */
	private String normalizedContent() {
		return content().replace("_", "");
	}

	@Override
	public String toString() {
		return value.content();
	}

	/**
	 * Check for a radix prefix after an optional sign.
	 *
	 * @param value
	 * 		Normalized literal content.
	 * @param prefix
	 * 		Prefix to test, such as {@code 0x} or {@code 0b}.
	 *
	 * @return {@code true} when the literal starts with the given prefix.
	 */
	private static boolean hasRadixPrefix(String value, String prefix) {
		return value.regionMatches(true, signOffset(value), prefix, 0, prefix.length());
	}

	/**
	 * @param value
	 * 		Normalized literal content.
	 *
	 * @return {@code true} when the literal is a hex floating-point value,
	 * identified by a {@code 0x} prefix and binary exponent marker.
	 */
	private static boolean isHexFloatingPoint(String value) {
		return hasRadixPrefix(value, "0x") && (value.indexOf('p') >= 0 || value.indexOf('P') >= 0);
	}

	/**
	 * @param value
	 * 		Normalized literal content.
	 *
	 * @return {@code true} when the literal should be treated as a decimal
	 * floating-point value rather than an integer.
	 */
	private static boolean isDecimalFloatingPoint(String value) {
		if (hasRadixPrefix(value, "0x") || hasRadixPrefix(value, "0b"))
			return false;
		int signOffset = signOffset(value);
		return value.indexOf('.', signOffset) >= 0
				|| value.indexOf('e', signOffset) >= 0
				|| value.indexOf('E', signOffset) >= 0
				|| value.endsWith("f")
				|| value.endsWith("F")
				|| value.endsWith("d")
				|| value.endsWith("D");
	}

	/**
	 * Parse a signed integer literal in the requested radix, optionally
	 * skipping a radix prefix and honoring the {@code l}/{@code L} wide suffix.
	 *
	 * @param value
	 * 		Normalized literal content.
	 * @param radix
	 * 		Integer radix to parse with.
	 * @param prefixLength
	 * 		Number of prefix characters after the optional sign.
	 *
	 * @return Parsed {@link Integer} or {@link Long}.
	 */
	private static Number parseInteger(String value, int radix, int prefixLength) {
		int signOffset = signOffset(value);
		int digitsStart = signOffset + prefixLength;
		String digits = value.substring(digitsStart);
		boolean wide = digits.endsWith("l") || digits.endsWith("L");
		if (wide)
			digits = digits.substring(0, digits.length() - 1);
		String signedDigits = value.substring(0, signOffset) + digits;
		if (wide)
			return Long.parseLong(signedDigits, radix);
		return Integer.parseInt(signedDigits, radix);
	}

	/**
	 * @param value
	 * 		Normalized literal content.
	 *
	 * @return Index immediately after an optional leading sign.
	 */
	private static int signOffset(String value) {
		if (value.startsWith("-") || value.startsWith("+"))
			return 1;
		return 0;
	}
}
