package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import org.jetbrains.annotations.NotNull;

/**
 * Resolution of an implemented interface reference on a class declaration.
 *
 * @param klass
 * 		The class declaration.
 * @param implemented
 * 		The resolved interface identifier.
 */
public record ClassImplements(@NotNull ASTClass klass,
                              @NotNull ASTIdentifier implemented) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return implemented;
	}
}
