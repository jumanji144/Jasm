package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Visitor for annotated elements.
 */
public interface ASTAnnotatedVisitor {
	/**
	 * Visits an annotation with its source-syntax visibility.
	 *
	 * @param visibility
	 * 		Visibility represented by the annotation's source syntax.
	 * @param classType
	 * 		Annotation type in internal-name form.
	 *
	 * @return Visitor for annotation elements, or {@code null} when unsupported.
	 */
	ASTAnnotationVisitor visitAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType);

	/**
	 * Visits a type annotation with its source-syntax visibility and location payload.
	 *
	 * @param visibility
	 * 		Visibility represented by the annotation's source syntax.
	 * @param classType
	 * 		Annotation type in internal-name form.
	 * @param typeRef
	 * 		Type reference value.
	 * @param typePath
	 * 		Type path value, or {@code null} if not present.
	 *
	 * @return Visitor for annotation elements.
	 * @throws UnsupportedOperationException if type annotations are unsupported.
	 */
	default ASTAnnotationVisitor visitTypeAnnotation(@NotNull AnnotationVisibility visibility,
	                                                  @NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef,
	                                                  @Nullable ASTIdentifier typePath) {
		throw new UnsupportedOperationException("Target does not support type annotations");
	}
}
