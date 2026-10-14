package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a type reference.
 *
 * @param parentClass
 * 		The class containing the reference, or {@code null} if the reference is in a static initializer.
 * @param method
 * 		The method containing the reference.
 * @param type
 * 		The identifier of the type reference.
 */
public record TypeReferenceResolution(@Nullable ASTClass parentClass,
                                      @Nullable ASTMethod method,
                                      @NotNull ASTIdentifier type) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return type;
	}
}
