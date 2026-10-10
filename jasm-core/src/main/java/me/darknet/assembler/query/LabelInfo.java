package me.darknet.assembler.query;

import me.darknet.assembler.ast.primitive.ASTLabel;
import org.jetbrains.annotations.NotNull;

/**
 * Information about a label, including its name, declaration, and whether it's a duplicate.
 *
 * @param name
 * 		The name of the label. This is the identifier used in the assembly code to refer to this label.
 * @param declaration
 * 		The AST node where this label is declared.
 * @param duplicate
 *        {@code true} if this label is a duplicate declaration, {@code false} otherwise.
 * 		For valid code there should only ever be one declaration of a label.
 */
public record LabelInfo(@NotNull String name, @NotNull ASTLabel declaration, boolean duplicate) {}
