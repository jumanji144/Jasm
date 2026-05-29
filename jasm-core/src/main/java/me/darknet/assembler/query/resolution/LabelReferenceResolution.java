package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.query.LabelUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a label reference.
 *
 * @param parentClass
 * 		The class containing the reference, or {@code null} if resolving at the top-level is a method.
 * @param method
 * 		The method containing the reference.
 * @param reference
 * 		The identifier of the label reference.
 * @param usage
 * 		The usage of the label reference.
 */
public record LabelReferenceResolution(@Nullable ASTClass parentClass,
                                       @NotNull ASTMethod method,
                                       @NotNull ASTIdentifier reference,
                                       @NotNull LabelUsage usage) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return reference;
	}
}
