package me.darknet.assembler.helper;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Helper for processing source text through tokenization, declaration parsing, and AST processing.
 */
public class Processor {
	/**
	 * Processes source text through tokenization, declaration parsing, and AST processing.
	 *
	 * @param code
	 * 		Source code to process.
	 * @param source
	 * 		Source name used in diagnostics.
	 * @param format
	 * 		Bytecode format to use while processing.
	 *
	 * @return Parsed and processed declarations, or a result containing diagnostics.
	 */
	public static Result<List<ASTElement>> processSourceResult(String code, String source, BytecodeFormat format) {
		var tokenResult = new Tokenizer().tokenize(source, code);
		if (tokenResult.hasErr())
			return new Result<>(null, tokenResult.errors(), tokenResult.getWarns());

		var declarationResult = new DeclarationParser().parseDeclarations(tokenResult.get());
		if (declarationResult.hasErr())
			return new Result<>(null, declarationResult.errors(), declarationResult.getWarns());

		var processedResult = new ASTProcessor(format).processAST(declarationResult.get());
		return new Result<>(processedResult.get(), processedResult.errors(), processedResult.getWarns());
	}

	/**
	 * Processes source text through tokenization, declaration parsing, and AST processing.
	 *
	 * @param code
	 * 		Source code to process.
	 * @param source
	 * 		Source name used in diagnostics.
	 * @param consumer
	 * 		Consumer for the processed declarations.
	 * @param error
	 * 		BiConsumer for errors and warnings encountered during processing.
	 * @param format
	 * 		Bytecode format to use while processing.
	 */
	public static void processSource(String code, String source, Consumer<List<ASTElement>> consumer,
	                                 BiConsumer<List<ASTElement>, List<Error>> error, BytecodeFormat format) {
		processSourceResult(code, source, format)
				.ifOk(consumer)
				.ifErr(error);
	}

	/**
	 * Processes source text through tokenization, declaration parsing, and AST processing.
	 *
	 * @param code
	 * 		Source code to process.
	 * @param source
	 * 		Source name used in diagnostics.
	 * @param consumer
	 * 		Consumer for the processed declarations.
	 * @param error
	 * 		Consumer for errors encountered during processing.
	 * @param format
	 * 		Bytecode format to use while processing.
	 */
	public static void processSource(String code, String source, Consumer<List<ASTElement>> consumer,
	                                 Consumer<List<Error>> error, BytecodeFormat format) {
		processSource(code, source, consumer, (unused, errors) -> error.accept(errors), format);
	}
}
