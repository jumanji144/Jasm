package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import org.jetbrains.annotations.NotNull;

/**
 * Resolution of a class declaration.
 *
 * @param klass
 * 		The class declaration.
 */
public record ClassResolution(@NotNull ASTClass klass) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return klass;
	}
}
