package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Visitor for annotated elements.
 */
public interface ASTAnnotatedVisitor {
	/**
	 * Visits an annotation retained at runtime.
	 *
	 * @param classType
	 * 		Annotation type in internal-name form.
	 *
	 * @return Visitor for annotation elements, or {@code null} when unsupported.
	 */
	ASTAnnotationVisitor visitVisibleAnnotation(@NotNull ASTIdentifier classType);

	/**
	 * Visits an annotation not retained at runtime.
	 *
	 * @param classType
	 * 		Annotation type in internal-name form.
	 *
	 * @return Visitor for annotation elements, or {@code null} when unsupported.
	 */
	ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType);

	/**
	 * Visits an annotation retained as a Dalvik system annotation.
	 *
	 * @param classType
	 * 		Annotation type in internal-name form.
	 *
	 * @return Visitor for annotation elements, or {@code null} when unsupported.
	 */
	default ASTAnnotationVisitor visitSystemAnnotation(@NotNull ASTIdentifier classType) {
		return null;
	}

	/**
	 * Visits a type annotation retained at runtime.
	 *
	 * @param classType
	 * 		Annotation type in internal-name form.
	 * @param typeRef
	 * 		Type reference value.
	 * @param typePath
	 * 		Type path value, or {@code null} if not present.
	 *
	 * @return Visitor for annotation elements, or {@code null} when unsupported.
	 */
	ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath);

	/**
	 * Visits a type annotation not retained at runtime.
	 *
	 * @param classType
	 * 		Annotation type in internal-name form.
	 * @param typeRef
	 * 		Type reference value.
	 * @param typePath
	 * 		Type path value, or {@code null} if not present.
	 *
	 * @return Visitor for annotation elements, or {@code null} when unsupported.
	 */
	ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath);
}
