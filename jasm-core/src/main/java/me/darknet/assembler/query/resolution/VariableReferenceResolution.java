package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.query.VariableUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a variable reference.
 *
 * @param parentClass
 * 		The class containing the reference, or {@code null} if the reference is in a static initializer.
 * @param method
 * 		The method containing the reference.
 * @param reference
 * 		The identifier of the variable reference.
 * @param usage
 * 		The usage of the variable reference.
 */
public record VariableReferenceResolution(@Nullable ASTClass parentClass,
                                          @NotNull ASTMethod method,
                                          @NotNull ASTIdentifier reference,
                                          @NotNull VariableUsage usage) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return reference;
	}
}
