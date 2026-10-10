package me.darknet.assembler.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.target.TargetContext;

import java.util.List;

/**
 * Utilities for tokenizing, parsing, and processing assembly source code in tests.
 */
public final class AssemblyParseFixture {
	public static final String STDIN = "<stdin>";

	private AssemblyParseFixture() {}

	/**
	 * @param source
	 * 		The JASM assembly source code to tokenize.
	 *
	 * @return Tokens and lexer diagnostics.
	 */
	public static Outcome<List<Token>> tokenize(String source) {
		return tokenize(STDIN, source);
	}

	/**
	 * @param sourceName
	 * 		The name of the source for diagnostics.
	 * @param source
	 * 		The JASM assembly source code to tokenize.
	 *
	 * @return Tokens and lexer diagnostics.
	 */
	public static Outcome<List<Token>> tokenize(String sourceName, String source) {
		return new Tokenizer().tokenize(sourceName, source);
	}

	/**
	 * @param source
	 * 		The JASM assembly source code to parse.
	 *
	 * @return Parsed elements and syntax diagnostics.
	 */
	public static Outcome<List<ASTElement>> parse(String source) {
		return parse(STDIN, source);
	}

	/**
	 * @param sourceName
	 * 		The name of the source for diagnostics.
	 * @param source
	 * 		The JASM assembly source code to parse.
	 *
	 * @return Parsed elements and syntax diagnostics.
	 */
	public static Outcome<List<ASTElement>> parse(String sourceName, String source) {
		Outcome<List<Token>> tokenResult = tokenize(sourceName, source);
		if (tokenResult.hasErrors())
			return Outcome.failure(tokenResult.diagnostics());
		return new DeclarationParser().parseAny(tokenResult.requireValue());
	}

	/**
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 *
	 * @return Processed AST and diagnostics using the fixture JVM target.
	 */
	public static Outcome<List<ASTElement>> processAst(String source) {
		return processAst(STDIN, source, FixtureTarget.JVM.context());
	}

	/**
	 * @param sourceName
	 * 		The name of the source for diagnostics.
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 *
	 * @return Processed AST using the fixture JVM target.
	 */
	public static Outcome<List<ASTElement>> processAst(String sourceName, String source) {
		return processAst(sourceName, source, FixtureTarget.JVM.context());
	}

	/**
	 * @param sourceName
	 * 		The name of the source for diagnostics.
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 * @param target
	 * 		Target services used during AST processing.
	 *
	 * @return Processed AST and target-validation diagnostics.
	 */
	public static Outcome<List<ASTElement>> processAst(String sourceName, String source, TargetContext target) {
		Outcome<List<ASTElement>> parseResult = parse(sourceName, source);
		if (parseResult.hasErrors())
			return Outcome.failure(parseResult.diagnostics());
		return new ASTProcessor(target).processAST(parseResult.requireValue());
	}

	/**
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 *
	 * @return Processed AST using the fixture JVM target.
	 */
	public static Outcome<List<ASTElement>> processDeclarations(String source) {
		return processDeclarations(STDIN, source, FixtureTarget.JVM.context());
	}

	/**
	 * @param sourceName
	 * 		The name of the source for diagnostics.
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 *
	 * @return Processed AST using the fixture JVM target.
	 */
	public static Outcome<List<ASTElement>> processDeclarations(String sourceName, String source) {
		return processDeclarations(sourceName, source, FixtureTarget.JVM.context());
	}

	/**
	 * @param sourceName
	 * 		The name of the source for diagnostics.
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 * @param target
	 * 		Target services used during AST processing.
	 *
	 * @return Processed AST and target-validation diagnostics.
	 */
	public static Outcome<List<ASTElement>> processDeclarations(String sourceName, String source, TargetContext target) {
		Outcome<List<Token>> tokenResult = tokenize(sourceName, source);
		if (tokenResult.hasErrors())
			return Outcome.failure(tokenResult.diagnostics());
		Outcome<List<ASTElement>> parseResult = new DeclarationParser().parseDeclarations(tokenResult.requireValue());
		if (parseResult.hasErrors())
			return Outcome.failure(parseResult.diagnostics());
		return new ASTProcessor(target).processAST(parseResult.requireValue());
	}
}
