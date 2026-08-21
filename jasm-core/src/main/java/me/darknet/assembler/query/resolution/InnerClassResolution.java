package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTInner;
import org.jetbrains.annotations.NotNull;

/**
 * Resolution of an inner-class declaration attached to a class.
 *
 * @param klass
 * 		The enclosing class declaration.
 * @param inner
 * 		The resolved inner-class entry.
 */
public record InnerClassResolution(@NotNull ASTClass klass,
                                   @NotNull ASTInner inner) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return inner;
	}
}
