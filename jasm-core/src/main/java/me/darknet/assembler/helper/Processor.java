package me.darknet.assembler.helper;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.target.TargetContext;

import java.util.List;
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
	 * @param target
	 * 		Target services used during AST processing.
	 *
	 * @return Parsed and processed declarations, or structured diagnostics.
	 */
	public static Outcome<List<ASTElement>> processSourceResult(String code, String source, TargetContext target) {
		var tokenResult = new Tokenizer().tokenize(source, code);
		if (tokenResult.hasErrors())
			return Outcome.failure(tokenResult.diagnostics());

		var declarationResult = new DeclarationParser().parseDeclarations(tokenResult.requireValue());
		if (declarationResult.hasErrors())
			return Outcome.failure(declarationResult.diagnostics());

		return new ASTProcessor(target).processAST(declarationResult.requireValue());
	}

	/**
	 * Processes source text and routes a successful AST or reported errors to their consumers.
	 *
	 * @param code
	 * 		Source code to process.
	 * @param source
	 * 		Source name used in diagnostics.
	 * @param consumer
	 * 		Consumer for successfully processed declarations.
	 * @param error
	 * 		Consumer for diagnostics when one or more errors occurred.
	 * @param target
	 * 		Target services used during AST processing.
	 */
	public static void processSource(String code, String source, Consumer<List<ASTElement>> consumer,
	                                 Consumer<List<Diagnostic>> error, TargetContext target) {
		Outcome<List<ASTElement>> outcome = processSourceResult(code, source, target);
		if (outcome.hasErrors())
			error.accept(outcome.diagnostics());
		else
			consumer.accept(outcome.requireValue());
	}
}
