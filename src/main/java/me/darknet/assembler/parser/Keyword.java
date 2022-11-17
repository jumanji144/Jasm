package me.darknet.assembler.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Keyword {
	KEYWORD_CLASS("class"),
	KEYWORD_METHOD("method"),
	KEYWORD_END("end"),
	KEYWORD_FIELD("field"),
	KEYWORD_STATIC("static"),
	KEYWORD_PUBLIC("public"),
	KEYWORD_PRIVATE("private"),
	KEYWORD_PROTECTED("protected"),
	KEYWORD_SYNCHRONIZED("synchronized"),
	KEYWORD_VOLATILE("volatile"),
	KEYWORD_TRANSIENT("transient"),
	KEYWORD_NATIVE("native"),
	KEYWORD_ABSTRACT("abstract"),
	KEYWORD_STRICT("strictfp"),
	KEYWORD_BRIDGE("bridge"),
	KEYWORD_VARARGS("varargs"),
	KEYWORD_SUPER("super"),
	KEYWORD_ENUM_ACCESS("enum"),
	KEYWORD_SYNTHETIC("synthetic"),
	KEYWORD_INTERFACE("interface"),
	KEYWORD_ANNOTATION_ACCESS("annotation-interface"),
	KEYWORD_EXTENDS("extends"),
	KEYWORD_IMPLEMENTS("implements"),
	KEYWORD_VERSION("version"),
	KEYWORD_SOURCE_FILE("sourcefile"),
	KEYWORD_INNER_CLASS("innerclass"),
	KEYWORD_NEST_HOST("nesthost"),
	KEYWORD_NEST_MEMBER("nestmember"),
	KEYWORD_PERMITTED_SUBCLASS("permittedsubclass"),
	KEYWORD_MODULE("module"),
	KEYWORD_OPENS("opens"),
	KEYWORD_REQUIRES("requires"),
	KEYWORD_EXPORTS("exports"),
	KEYWORD_USES("uses"),
	KEYWORD_PROVIDES("provides"),
	KEYWORD_MAIN_CLASS("mainclass"),
	KEYWORD_PACKAGE("package"),
	KEYWORD_TO("to"),
	KEYWORD_WITH("with"),
	KEYWORD_ENCLOSING_METHOD("enclosingmethod"),
	KEYWORD_FINAL("final"),
	KEYWORD_STACK("stack"),
	KEYWORD_LOCALS("locals"),
	KEYWORD_SWITCH("lookupswitch", true),
	KEYWORD_TABLESWITCH("tableswitch", true),
	KEYWORD_DEFAULT("default"),
	KEYWORD_CASE("case"),
	KEYWORD_MACRO("macro"),
	KEYWORD_CATCH("catch"),
	KEYWORD_HANDLE("handle"),
	KEYWORD_ARGS("args"),
	KEYWORD_TYPE("type"),
	KEYWORD_METHOD_TYPE("method-type"),
	KEYWORD_ANNOTATION("annotation"),
	KEYWORD_INVISIBLE_ANNOTATION("invisible-annotation"),
	KEYWORD_PARAMETER_ANNOTATION("parameter-annotation"),
	KEYWORD_INVISIBLE_PARAMETER_ANNOTATION("invisible-parameter-annotation"),
	KEYWORD_TYPE_ANNOTATION("type-annotation"),
	KEYWORD_INVISIBLE_TYPE_ANNOTATION("invisible-type-annotation"),
	KEYWORD_ENUM("annotation-enum"),
	KEYWORD_SIGNATURE("signature"),
	KEYWORD_THROWS("throws"),
	KEYWORD_EXPR("expr");

	public static final Map<String, Keyword> textMapping = new HashMap<>();
	public static final List<Keyword> accessModifiers = Arrays.asList(
			KEYWORD_PUBLIC,
			KEYWORD_PRIVATE,
			KEYWORD_PROTECTED,
			KEYWORD_STATIC,
			KEYWORD_FINAL,
			KEYWORD_BRIDGE,
			KEYWORD_SYNCHRONIZED,
			KEYWORD_SYNTHETIC,
			KEYWORD_NATIVE,
			KEYWORD_ABSTRACT,
			KEYWORD_STRICT,
			KEYWORD_VARARGS,
			KEYWORD_VOLATILE,
			KEYWORD_TRANSIENT,
			KEYWORD_ENUM_ACCESS,
			KEYWORD_SUPER
	);
	private final String text;
	private final boolean exempt;

	Keyword(String text, boolean exempt) {
		this.text = text;
		this.exempt = exempt;
	}

	Keyword(String text) {
		this(text, false);
	}

	/**
	 * @param text
	 * 		Text of a keyword's {@link #getText() literal value}.
	 *
	 * @return Matching keyword instance, or {@code null} for no match.
	 */
	public static Keyword fromString(String text) {
		return textMapping.get(text);
	}

	/**
	 * @return Literal name.
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return {@code true} if the keyword is exempted from requiring a {@link Keywords#getPrefix() prefix}.
	 */
	public boolean isPrefixExempt() {
		return exempt;
	}

	/**
	 * @param keywords
	 * 		Keywords configuration instance. May specify things such as a prefix.
	 *
	 * @return Formatted name.
	 */
	public String toString(Keywords keywords) {
		if (exempt)
			return getText();
		return keywords.toString(this);
	}

	@Override
	public String toString() {
		return text;
	}

	static {
		for (Keyword keyword : Keyword.values()) {
			textMapping.put(keyword.getText(), keyword);
		}
	}
}
