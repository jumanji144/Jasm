package me.darknet.assembler.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;

import java.util.List;

/**
 * Utilities for tokenizing, parsing, and processing assembly source code in tests.
 */
public final class AssemblyParseFixture {
	private static final String STDIN = "<stdin>";

	private AssemblyParseFixture() {}

	/**
	 * @param source
	 * 		The JASM assembly source code to tokenize.
	 *
	 * @return Result of tokenization on success, or a list of errors on failure.
	 */
	public static Result<List<Token>> tokenize(String source) {
		return tokenize(STDIN, source);
	}

	/**
	 * @param sourceName
	 * 		The name of the source (File name) for error reporting.
	 * @param source
	 * 		The JASM assembly source code to tokenize.
	 *
	 * @return Result of tokenization on success, or a list of errors on failure.
	 */
	public static Result<List<Token>> tokenize(String sourceName, String source) {
		return new Tokenizer().tokenize(sourceName, source);
	}

	/**
	 * @param source
	 * 		The JASM assembly source code to parse.
	 *
	 * @return Result of parsing on success, or a list of errors on failure.
	 */
	public static Result<List<ASTElement>> parse(String source) {
		return parse(STDIN, source);
	}

	/**
	 * @param sourceName
	 * 		The name of the source (File name) for error reporting.
	 * @param source
	 * 		The JASM assembly source code to parse.
	 *
	 * @return Result of parsing on success, or a list of errors on failure.
	 */
	public static Result<List<ASTElement>> parse(String sourceName, String source) {
		Result<List<Token>> tokenResult = tokenize(sourceName, source);
		if (tokenResult.hasErr())
			return Result.err(tokenResult.errors());
		return new DeclarationParser().parseAny(tokenResult.get());
	}

	/**
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 *
	 * @return Result of processing on success, or a list of errors on failure.
	 */
	public static Result<List<ASTElement>> processAst(String source) {
		return processAst(STDIN, source, BytecodeFormat.DEFAULT);
	}

	/**
	 * @param sourceName
	 * 		The name of the source (File name) for error reporting.
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 * @param format
	 * 		The bytecode format to target during AST processing.
	 *
	 * @return Result of processing on success, or a list of errors on failure.
	 */
	public static Result<List<ASTElement>> processAst(String sourceName, String source, BytecodeFormat format) {
		Result<List<ASTElement>> parseResult = parse(sourceName, source);
		if (parseResult.hasErr())
			return Result.err(parseResult.errors());
		return new ASTProcessor(format).processAST(parseResult.get());
	}

	/**
	 * @param sourceName
	 * 		The name of the source (File name) for error reporting.
	 * @param source
	 * 		The JASM assembly source code to process into AST elements.
	 * @param format
	 * 		The bytecode format to target during AST processing.
	 *
	 * @return Result of processing on success, or a list of errors on failure.
	 */
	public static Result<List<ASTElement>> processDeclarations(String sourceName, String source, BytecodeFormat format) {
		Result<List<Token>> tokenResult = tokenize(sourceName, source);
		if (tokenResult.hasErr())
			return Result.err(tokenResult.errors());
		Result<List<ASTElement>> parseResult = new DeclarationParser().parseDeclarations(tokenResult.get());
		if (parseResult.hasErr())
			return Result.err(parseResult.errors());
		return new ASTProcessor(format).processAST(parseResult.get());
	}
}
