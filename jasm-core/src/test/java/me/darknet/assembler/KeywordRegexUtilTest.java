package me.darknet.assembler;

import me.darknet.assembler.parser.processor.DeclarationRegistry;
import me.darknet.assembler.parser.processor.ProcessorKeywords;
import me.darknet.assembler.test.FixtureTarget;
import me.darknet.assembler.util.KeywordRegexUtil;
import me.darknet.assembler.visitor.Modifiers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeywordRegexUtilTest {

	@Test
	public void matchesRegisteredJvmKeywords() {
		var target = FixtureTarget.JVM.context();
		Pattern pattern = Pattern.compile(KeywordRegexUtil.getKeywordRegex(target));

		for (String keyword : target.keywordNames())
			assertMatches(pattern, keyword);

		for (String keyword : DeclarationRegistry.createDefault().getKeywords())
			assertMatches(pattern, keyword);

		for (String keyword : Modifiers.getValidModifiers())
			assertMatches(pattern, keyword);

		for (String keyword : ProcessorKeywords.getCommonMethodBodyKeywords())
			assertMatches(pattern, keyword);

		assertFalse(pattern.matcher("move").find());
	}

	@Test
	public void matchesRegisteredDalvikKeywords() {
		var target = FixtureTarget.DALVIK.context();
		Pattern pattern = Pattern.compile(KeywordRegexUtil.getKeywordRegex(target));

		for (String keyword : target.keywordNames())
			assertMatches(pattern, keyword);

		assertFalse(pattern.matcher("aconst_null").find());
		assertFalse(Pattern.compile(KeywordRegexUtil.getKeywordRegex(FixtureTarget.JVM.context()))
				.matcher("registers").find());
	}

	/**
	 * Ensures longer Dalvik instruction names are not reported as shorter registered prefixes.
	 */
	@Test
	public void matchesCompleteDalvikInstructionNames() {
		Pattern pattern = Pattern.compile(KeywordRegexUtil.getKeywordRegex(FixtureTarget.DALVIK.context()));

		for (String instruction : List.of("move", "move-result", "move-result-object")) {
			var matcher = pattern.matcher(instruction);
			assertTrue(matcher.find(), instruction);
			assertEquals(instruction, matcher.group(), instruction);
		}
	}

	@Test
	public void factorsCommonPrefixesAndSuffixes() {
		List<String> keywords = List.of(
				"astore", "aastore", "istore", "fstore", "dstore", "lstore",
				"invokevirtual", "invokespecial", "invokestatic"
		);
		String regex = KeywordRegexUtil.getKeywordRegex(keywords);
		String unoptimized = "\\b(?:" + String.join("|", keywords) + ")\\b";

		assertTrue(regex.length() < unoptimized.length());
		Pattern pattern = Pattern.compile(regex);
		for (String keyword : keywords)
			assertMatches(pattern, keyword);
	}

	@Test
	public void escapesRegexSyntaxInCustomKeywords() {
		Pattern pattern = Pattern.compile(KeywordRegexUtil.getKeywordRegex(List.of("a+b", "a.b", "a/b", "a[b]")));

		assertMatches(pattern, "a+b");
		assertMatches(pattern, "a.b");
		assertMatches(pattern, "a/b");
		assertMatches(pattern, "a[b]");
		assertFalse(pattern.matcher("ab").find());
	}

	@Test
	public void emptyKeywordCollectionNeverMatches() {
		Pattern pattern = Pattern.compile(KeywordRegexUtil.getKeywordRegex(List.of()));

		assertFalse(pattern.matcher("class").find());
	}

	private static void assertMatches(Pattern pattern, String keyword) {
		assertTrue(pattern.matcher(" " + keyword + " ").find(), keyword);
	}
}
