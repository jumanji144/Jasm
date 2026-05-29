package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for a declaration.
 */
@FunctionalInterface
public interface DeclarationHandler {
	/**
	 * Parse the given declaration.
	 *
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed AST element, or {@code null} if the declaration should be ignored.
	 */
	@Nullable
	ASTElement parse(@NotNull ProcessorContext context, @NotNull ASTDeclaration declaration);
}
