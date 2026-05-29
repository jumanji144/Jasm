package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Processor for an AST.
 */
public class ASTProcessor {
	private final BytecodeFormat format;
	private final DeclarationRegistry registry;

	/**
	 * @param format
	 * 		Format to process the AST for.
	 */
	public ASTProcessor(@NotNull BytecodeFormat format) {
		this.format = format;
		this.registry = DeclarationRegistry.createDefault();
	}

	/**
	 * Process the given AST and return the result.
	 *
	 * @param ast
	 * 		AST to process.
	 *
	 * @return Result of processing the AST, containing the processed AST elements
	 * and any errors or warnings that occurred during processing.
	 */
	public @NotNull Result<List<ASTElement>> processAST(@NotNull List<ASTElement> ast) {
		ProcessorContext context = new ProcessorContext(format, registry);
		for (ASTElement element : ast) {
			if (element instanceof ASTDeclaration declaration) {
				ASTElement parsed = context.parseDeclaration(declaration);
				if (parsed != null) {
					context.add(parsed);
				}
			} else {
				context.throwUnexpectedElementError("declaration", element);
			}
		}
		return new Result<>(context.getResult(), context.getErrors(), context.getWarns());
	}
}
