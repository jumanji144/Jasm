package me.darknet.assembler.util;

import me.darknet.assembler.parser.processor.DeclarationRegistry;
import me.darknet.assembler.parser.processor.ProcessorKeywords;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Builds compact regular expressions for the keywords accepted by JASM.
 */
public final class KeywordRegexUtil {
	private static final String NO_KEYWORDS_REGEX = "\\b(?!)\\b";

	private KeywordRegexUtil() {}

	/**
	 * @return Set of keywords shared by all supported formats.
	 */
	private static @NotNull Set<String> getCommonKeywords() {
		Set<String> keywords = new HashSet<>(DeclarationRegistry.createDefault().getKeywords());
		keywords.addAll(Modifiers.getValidModifiers());
		keywords.addAll(ProcessorKeywords.getCommonMethodBodyKeywords());
		return keywords;
	}

	/**
	 * Builds the keyword expression from common JASM keywords and the active target's services.
	 *
	 * @param target
	 * 		Target providing instruction and method-attribute keywords.
	 *
	 * @return A word-boundary regular expression matching the target's JASM keywords.
	 */
	public static @NotNull String getKeywordRegex(@NotNull TargetContext target) {
		Set<String> keywords = getCommonKeywords();
		keywords.addAll(target.keywordNames());
		return getKeywordRegex(keywords);
	}

	/**
	 * Builds the keyword expression from common JASM keywords and the active targets' services.
	 *
	 * @param targets
	 * 		Targets providing instruction and method-attribute keywords.
	 *
	 * @return A word-boundary regular expression matching the targets' JASM keywords.
	 */
	public static @NotNull String getKeywordRegex(@NotNull TargetContext[] targets) {
		Set<String> keywords = getCommonKeywords();
		for (TargetContext target : targets)
			keywords.addAll(target.keywordNames());
		return getKeywordRegex(keywords);
	}

	/**
	 * Builds a compact word-boundary regular expression for an arbitrary keyword collection.
	 * <p>
	 * Common prefixes and suffixes are factored, and terminal branches are represented as optional
	 * expressions where that produces a shorter equivalent rule. JASM keywords use {@code \\b};
	 * custom values with punctuation use equivalent non-word lookarounds instead.
	 *
	 * @param keywords
	 * 		Keywords to include. Duplicate values are ignored.
	 *
	 * @return A regular expression matching exactly the supplied keywords at non-word boundaries.
	 *
	 * @throws NullPointerException
	 * 		If {@code keywords}, or an element of it, is {@code null}.
	 * @throws IllegalArgumentException
	 * 		If a keyword is empty.
	 */
	public static @NotNull String getKeywordRegex(@NotNull Collection<String> keywords) {
		// Normalize once so the optimizer has deterministic input and never emits duplicate alternatives.
		NavigableSet<String> normalized = new TreeSet<>();
		for (String keyword : keywords) {
			if (keyword == null || keyword.isEmpty())
				throw new IllegalArgumentException("keywords contains an null or empty value");
			normalized.add(keyword);
		}
		if (normalized.isEmpty())
			return NO_KEYWORDS_REGEX;

		// Factor the finite keyword language in both directions and keep the shorter equivalent expression.
		Expression expression = new RegexBuilder().build(normalized);
		boolean useWordBoundaries = hasWordBoundaries(normalized);
		String nonWordCharacters = getNonWordCharacterClass(normalized);
		String boundary = getKeywordBoundary(useWordBoundaries, nonWordCharacters, true);
		String trailingBoundary = getKeywordBoundary(useWordBoundaries, nonWordCharacters, false);
		return boundary + "(?:" + expression.source() + ")" + trailingBoundary;
	}

	private static @NotNull String getKeywordBoundary(boolean useWordBoundaries,
	                                                  String nonWordCharacters, boolean leading) {
		String boundary = useWordBoundaries
				? "\\b"
				: leading ? "(?<!\\w)" : "(?!\\w)";
		if (nonWordCharacters.isEmpty())
			return boundary;

		// Punctuation inside a registered keyword must not become a boundary for a shorter keyword.
		return boundary + (leading
				? "(?<!" + nonWordCharacters + ")"
				: "(?!" + nonWordCharacters + ")");
	}

	private static @NotNull String getNonWordCharacterClass(Collection<String> keywords) {
		NavigableSet<Character> nonWordCharacters = new TreeSet<>();
		for (String keyword : keywords) {
			for (int i = 0; i < keyword.length(); i++) {
				char value = keyword.charAt(i);
				if (!isWordCharacter(value))
					nonWordCharacters.add(value);
			}
		}
		if (nonWordCharacters.isEmpty())
			return "";

		StringBuilder result = new StringBuilder("[");
		for (char value : nonWordCharacters)
			result.append(escapeCharacterClass(value));
		return result.append(']').toString();
	}

	private static boolean hasWordBoundaries(Collection<String> keywords) {
		for (String keyword : keywords) {
			if (!isWordCharacter(keyword.charAt(0)) || !isWordCharacter(keyword.charAt(keyword.length() - 1))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isWordCharacter(char value) {
		return Character.isLetterOrDigit(value) || value == '_';
	}

	private static final class RegexBuilder {
		private final Map<List<String>, Expression> cache = new HashMap<>();

		private Expression build(Collection<String> words) {
			List<String> sorted = words.stream().sorted().toList();
			return buildSorted(sorted);
		}

		private Expression buildSorted(List<String> words) {
			List<String> key = List.copyOf(words);
			Expression cached = cache.get(key);
			if (cached != null) {
				return cached;
			}

			// A single word has no shared structure left to factor.
			if (words.size() == 1) {
				Expression result = literal(words.getFirst());
				cache.put(key, result);
				return result;
			}

			// Keep the empty suffix separate so it can become an optional branch.
			List<String> nonEmpty = new ArrayList<>(words.size());
			boolean hasEmpty = false;
			for (String word : words) {
				if (word.isEmpty()) {
					hasEmpty = true;
				} else {
					nonEmpty.add(word);
				}
			}

			List<Expression> candidates = new ArrayList<>(2);
			if (hasEmpty) {
				candidates.add(optional(build(nonEmpty)));
			} else {
				candidates.add(buildByFirst(nonEmpty));
				candidates.add(buildByLast(nonEmpty));
			}

			// Prefix and suffix factoring compete because either direction can be substantially smaller.
			Expression result = shortest(candidates);
			cache.put(key, result);
			return result;
		}

		private Expression buildByFirst(List<String> words) {
			NavigableMap<Character, List<String>> groups = new TreeMap<>();
			for (String word : words) {
				groups.computeIfAbsent(word.charAt(0), ignored -> new ArrayList<>())
						.add(word.substring(1));
			}

			List<Expression> branches = new ArrayList<>(groups.size());
			for (var entry : groups.entrySet()) {
				branches.add(concat(literal(entry.getKey()), build(entry.getValue())));
			}
			return choice(branches);
		}

		private Expression buildByLast(List<String> words) {
			NavigableMap<Character, List<String>> groups = new TreeMap<>();
			for (String word : words) {
				int lastIndex = word.length() - 1;
				groups.computeIfAbsent(word.charAt(lastIndex), ignored -> new ArrayList<>())
						.add(word.substring(0, lastIndex));
			}

			List<Expression> branches = new ArrayList<>(groups.size());
			for (var entry : groups.entrySet()) {
				branches.add(concat(build(entry.getValue()), literal(entry.getKey())));
			}
			return choice(branches);
		}

		private static Expression shortest(List<Expression> candidates) {
			Expression shortest = candidates.getFirst();
			for (int i = 1; i < candidates.size(); i++) {
				Expression candidate = candidates.get(i);
				if (candidate.source().length() < shortest.source().length()
						|| candidate.source().length() == shortest.source().length()
						&& candidate.source().compareTo(shortest.source()) < 0) {
					shortest = candidate;
				}
			}
			return shortest;
		}

		private static Expression choice(List<Expression> branches) {
			if (branches.size() == 1) {
				return branches.getFirst();
			}
			List<String> sources = branches.stream().map(Expression::source).toList();
			return new Expression("(?:" + String.join("|", sources) + ")", true);
		}

		private static Expression optional(Expression expression) {
			if (expression.atomic()) {
				return new Expression(expression.source() + "?", true);
			}
			return new Expression("(?:" + expression.source() + ")?", true);
		}

		private static Expression concat(Expression left, Expression right) {
			if (left.source().isEmpty()) {
				return right;
			}
			if (right.source().isEmpty()) {
				return left;
			}
			return new Expression(left.source() + right.source(), false);
		}

		private static Expression literal(String value) {
			Expression result = new Expression("", true);
			for (int i = 0; i < value.length(); i++) {
				result = concat(result, literal(value.charAt(i)));
			}
			return result;
		}

		private static Expression literal(char value) {
			return new Expression(escape(value), true);
		}

		private static String escape(char value) {
			return switch (value) {
				case '\\', '^', '$', '.', '|', '?', '*', '+', '(', ')', '[', ']', '{', '}' -> "\\" + value;
				default -> String.valueOf(value);
			};
		}
	}

	private static String escapeCharacterClass(char value) {
		return switch (value) {
			case '\\', '^', '-', '[', ']' -> "\\" + value;
			default -> String.valueOf(value);
		};
	}

	private record Expression(String source, boolean atomic) {}
}
