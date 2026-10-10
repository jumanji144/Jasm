package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import org.jetbrains.annotations.NotNull;

/**
 * Resolution of a standalone annotation declaration.
 *
 * @param annotation
 * 		The resolved annotation.
 */
public record IndependentAnnotationResolution(@NotNull ASTAnnotation annotation) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return annotation;
	}
}
