package me.darknet.assembler.parser;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration for keywords and how they are handled in a {@link Parser}
 */
public class Keywords {
	private final Set<String> exemptCache;
	private Set<String> modifiersCache;
	private String prefix;

	/**
	 * Create a default keywords instance with no prefix.
	 */
	public Keywords() {
		this(null);
	}

	/**
	 * Create a keywords instance that enforces a prefix on keywords.
	 *
	 * @param prefix
	 * 		Prefix to apply to keywords.
	 */
	public Keywords(String prefix) {
		this.prefix = prefix;
		exemptCache = Arrays.stream(Keyword.values())
				.filter(Keyword::isPrefixExempt)
				.map(Keyword::getText)
				.collect(Collectors.toSet());
		updateModifierCache();
	}

	/**
	 * @return Keyword prefix.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @param prefix
	 * 		New keyword prefix.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
		updateModifierCache();
	}

	/**
	 * Update the {@link #modifiersCache} with the current {@link #prefix},
	 * which is used by {@link #isAccessModifier(String)}.
	 */
	private void updateModifierCache() {
		modifiersCache = Keyword.accessModifiers.stream()
				.map(s -> s.toString(this))
				.collect(Collectors.toSet());
	}

	/**
	 * @param keyword
	 * 		Keyword instance.
	 *
	 * @return String representation, which will prepend the prefix if one exists.
	 */
	public String toString(Keyword keyword) {
		String text = keyword.getText();
		if (prefix == null)
			return text;
		return prefix + text;
	}

	/**
	 * @param group
	 * 		Group instance.
	 *
	 * @return Matching keywords <i>(Prefix required if specified)</i> from the {@link Group#content() group text content}.
	 */
	public Keyword fromGroup(Group group) {
		return fromString(group.content());
	}

	/**
	 * @param token
	 * 		Token instance.
	 *
	 * @return Matching keywords <i>(Prefix required if specified)</i> from the {@link Token#content token text content}.
	 */
	public Keyword fromToken(Token token) {
		return fromString(token.content);
	}

	/**
	 * @param text
	 * 		Text content.
	 *
	 * @return Matching keywords <i>(Prefix required if specified)</i> from the text.
	 */
	public Keyword fromString(String text) {
		if (prefix == null || prefix.isEmpty()) {
			return Keyword.fromString(text);
		} else {
			// A few keywords bypass the prefix.
			// Hacky, but necessary unfortunately
			if (exemptCache.contains(text))
				return Keyword.fromString(text);
			// Chop off the prefix and look up the text for a matching prefix.
			text = text.substring(prefix.length());
			return Keyword.fromString(text);
		}
	}

	/**
	 * @param token
	 * 		Token instance.
	 *
	 * @return {@code true} if it matches an access modifier keyword.
	 */
	public boolean isAccessModifier(Token token) {
		return isAccessModifier(token.content);
	}

	/**
	 * @param content
	 * 		Text.
	 *
	 * @return {@code true} if it matches an access modifier keyword.
	 */
	public boolean isAccessModifier(String content) {
		return modifiersCache.contains(content);
	}

	/**
	 * @param key
	 * 		Keyword to match.
	 * @param group
	 * 		Group to match against, using {@link Group#content() the text content}.
	 *
	 * @return {@code true} for keyword match.
	 */
	public boolean match(Keyword key, Group group) {
		return match(key, group.content());
	}

	/**
	 * @param key
	 * 		Keyword to match.
	 * @param token
	 * 		Token to match against, using {@link Token#content the text content}.
	 *
	 * @return {@code true} for keyword match.
	 */
	public boolean match(Keyword key, Token token) {
		return match(key, token.content);
	}

	/**
	 * @param key
	 * 		Keyword to match.
	 * @param content
	 * 		Text to match against.
	 *
	 * @return {@code true} for keyword match.
	 */
	public boolean match(Keyword key, String content) {
		return fromString(content) == key;
	}
}
