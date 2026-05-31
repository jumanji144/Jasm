package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of an annotation attached to a method declaration.
 *
 * @param parentClass
 * 		The class containing the method, or {@code null} if the method is top-level.
 * @param targetMethod
 * 		The method the annotation is attached to.
 * @param annotation
 * 		The resolved annotation.
 */
public record MethodAnnotationResolution(@Nullable ASTClass parentClass,
                                         @NotNull ASTMethod targetMethod,
                                         @NotNull ASTAnnotation annotation) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return annotation;
	}
}
