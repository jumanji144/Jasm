package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import org.jetbrains.annotations.NotNull;

/**
 * Resolution of a superclass reference on a class declaration.
 *
 * @param klass
 * 		The class declaration.
 * @param superName
 * 		The resolved superclass identifier.
 */
public record ClassExtends(@NotNull ASTClass klass,
                           @NotNull ASTIdentifier superName) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return superName;
	}
}
