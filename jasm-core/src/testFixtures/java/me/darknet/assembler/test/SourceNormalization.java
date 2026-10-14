package me.darknet.assembler.test;

import java.util.regex.Pattern;

/**
 * Utilities for normalizing assembly source code in tests.
 */
public final class SourceNormalization {
	private static final Pattern DUPLICATE_NEWLINES = Pattern.compile("\\n\\s*\\n");
	private static final Pattern END_LINE_PADDING = Pattern.compile("[ \\t]+\\n");
	private static final Pattern COMMENTS = Pattern.compile("(?:^|\\n)\\s*//.+");

	private SourceNormalization() {}

	/**
	 * Transforms the given input assembly source code into a normalized form by:
	 * <ul>
	 *     <li>Removing carriage return characters.</li>
	 *     <li>Collapsing multiple consecutive spaces into a single space.</li>
	 *     <li>Removing comments (lines starting with //).</li>
	 *     <li>Removing trailing whitespace at the end of lines.</li>
	 *     <li>Collapsing multiple consecutive newlines into a single newline.</li>
	 * </ul>
	 * Note, that in some cases this can break expectations,
	 * like if you have a LDC instruction with a string literal that contains multiple spaces.
	 *
	 * @param input
	 * 		The JASM assembly source code to normalize.
	 *
	 * @return The normalized assembly source code.
	 */
	public static String normalize(String input) {
		input = input.replace("\r", "");
		while (input.contains("  "))
			input = input.replace("  ", " ");

		input = COMMENTS.matcher(input).replaceAll("");
		input = END_LINE_PADDING.matcher(input).replaceAll("\n");
		input = DUPLICATE_NEWLINES.matcher(input).replaceAll("\n");
		return input.trim();
	}
}
