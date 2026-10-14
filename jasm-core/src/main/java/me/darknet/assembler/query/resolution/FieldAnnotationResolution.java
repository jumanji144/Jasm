package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of an annotation attached to a field declaration.
 *
 * @param parentClass
 * 		The class containing the field, or {@code null} if the field is top-level.
 * @param targetField
 * 		The field the annotation is attached to.
 * @param annotation
 * 		The resolved annotation.
 */
public record FieldAnnotationResolution(@Nullable ASTClass parentClass,
                                        @NotNull ASTField targetField,
                                        @NotNull ASTAnnotation annotation) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return annotation;
	}
}
