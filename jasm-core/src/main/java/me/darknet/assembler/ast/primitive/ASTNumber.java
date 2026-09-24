package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

/**
 * Numeric literal node supporting integer and floating-point source spellings.
 */
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
		if (isBitPattern()) {
			// # prefix indicates a raw floating-point bit pattern, which is always a float or double.
			return parseBitPattern(value);
		} else if (lower.startsWith("nan")) {
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
		if (isBitPattern()) {
			int digits = bitPatternDigitCount(content());
			return digits == 16 || digits == 64;
		}
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
		if (isBitPattern())
			return true;
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
		if (isBitPattern()) {
			return isWide() ? Double.isNaN(number().doubleValue()) : Float.isNaN(number().floatValue());
		}
		String value = content().toLowerCase();
		return value.equals("nan") || value.equals("nand") || value.equals("nanf");
	}

	/**
	 * @return {@code true} when this literal is {@code Infinity}.
	 */
	public boolean isInfinity() {
		if (isBitPattern()) {
			return isWide() ? Double.isInfinite(number().doubleValue()) : Float.isInfinite(number().floatValue());
		}
		String value = content().toLowerCase();
		return value.equals("infinity") || value.equals("+infinity") || value.equals("-infinity") ||
				value.equals("infinityd") || value.equals("+infinityd") || value.equals("-infinityd") ||
				value.equals("infinityf") || value.equals("+infinityf") || value.equals("-infinityf");
	}

	/**
	 * Check whether this literal has the fixed-width raw floating-point bit-pattern spelling.
	 *
	 * @return {@code true} when this literal is one of the accepted {@code #0x} or {@code #0b}
	 *         bit patterns.
	 */
	public boolean isBitPattern() {
		return isBitPatternLiteral(content());
	}

	/**
	 * @param value
	 * 		Raw literal content, including any underscore separators.
	 *
	 * @return {@code true} when {@code value} is a valid fixed-width raw bit-pattern literal.
	 */
	private static boolean isBitPatternLiteral(String value) {
		if (value == null || value.length() <= 3 || value.charAt(0) != '#' || value.charAt(1) != '0')
			return false;

		char prefix = Character.toLowerCase(value.charAt(2));
		int radix;
		int narrowDigits;
		int wideDigits;
		if (prefix == 'x') {
			radix = 16;
			narrowDigits = 8;
			wideDigits = 16;
		} else if (prefix == 'b') {
			radix = 2;
			narrowDigits = 32;
			wideDigits = 64;
		} else {
			return false;
		}

		if (value.charAt(3) == '_' || value.charAt(value.length() - 1) == '_')
			return false;
		int digitCount = bitPatternDigitCount(value);
		for (int index = 3; index < value.length(); index++) {
			char digit = value.charAt(index);
			if (digit != '_' && Character.digit(digit, radix) < 0)
				return false;
		}
		return digitCount == narrowDigits || digitCount == wideDigits;
	}

	/**
	 * Count the non-separator payload digits in a raw bit-pattern spelling.
	 *
	 * @param value
	 * 		Raw literal content.
	 *
	 * @return Number of hexadecimal or binary payload digits after the {@code #0x}/{@code #0b} prefix.
	 */
	private static int bitPatternDigitCount(String value) {
		int count = 0;
		for (int index = 3; index < value.length(); index++) {
			if (value.charAt(index) != '_')
				count++;
		}
		return count;
	}

	/**
	 * @param value
	 * 		Normalized raw literal content.
	 *
	 * @return Decoded {@link Float} or {@link Double} represented by {@code value}.
	 *
	 * @throws NumberFormatException
	 * 		When {@code value} is not one of the supported fixed-width bit patterns.
	 */
	private static Number parseBitPattern(String value) {
		char prefix = Character.toLowerCase(value.charAt(2));
		int radix = prefix == 'x' ? 16 : 2;
		String digits = value.substring(3);
		return switch (digits.length()) {
			case 8, 32 -> Float.intBitsToFloat(Integer.parseUnsignedInt(digits, radix));
			case 16, 64 -> Double.longBitsToDouble(Long.parseUnsignedLong(digits, radix));
			default -> throw new NumberFormatException("Invalid raw floating-point bit-pattern literal: " + value);
		};
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
