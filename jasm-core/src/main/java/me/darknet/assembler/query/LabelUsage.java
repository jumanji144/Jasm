package me.darknet.assembler.query;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a usage of a label in the code.
 *
 * @param label
 * 		The resolved label information, if available. May be {@code null} if the label could not be resolved.
 * @param name
 * 		The name of the label being referenced.
 * @param reference
 * 		The AST node where the label is being referenced.
 * @param kind
 * 		The kind of label reference (jump target, switch case, etc.)
 * @param context
 * 		The context in which the label is being referenced (Like the instruction or switch statement), if applicable.
 * @param ambiguous
 * 		Whether the reference is ambiguous (Multiple labels with the same name were found in the scope).
 */
public record LabelUsage(@Nullable LabelInfo label,
                         @NotNull String name,
                         @NotNull ASTIdentifier reference,
                         @NotNull LabelReferenceKind kind,
                         @Nullable String context,
                         boolean ambiguous) {
	public boolean resolved() {
		return label != null;
	}
}
