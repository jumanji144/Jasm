package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a method declaration.
 *
 * @param parentClass
 * 		The class containing the method, or {@code null} if resolving at the top-level is a method.
 * @param method
 * 		The method declaration.
 */
public record MethodResolution(@Nullable ASTClass parentClass,
                               @NotNull ASTMethod method) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return method;
	}
}
