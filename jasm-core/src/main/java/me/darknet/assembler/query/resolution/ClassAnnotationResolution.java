package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import org.jetbrains.annotations.NotNull;

/**
 * Resolution of an annotation attached to a class declaration.
 *
 * @param targetClass
 * 		The class the annotation is attached to.
 * @param annotation
 * 		The resolved annotation.
 */
public record ClassAnnotationResolution(@NotNull ASTClass targetClass,
                                        @NotNull ASTAnnotation annotation) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return annotation;
	}
}
