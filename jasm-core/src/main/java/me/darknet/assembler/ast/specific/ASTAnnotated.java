package me.darknet.assembler.ast.specific;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * AST model of an element that can have annotations.
 */
public interface ASTAnnotated {
	/**
	 * @return List of visible annotations on the element.
	 */
	@NotNull
	List<ASTAnnotation> getVisibleAnnotations();

	/**
	 * @return List of invisible annotations on the element.
	 */
	@NotNull
	List<ASTAnnotation> getInvisibleAnnotations();

	/**
	 * @return List of visible type annotations on the element.
	 */
	@NotNull
	List<ASTAnnotation> getVisibleTypeAnnotations();

	/**
	 * @return List of invisible type annotations on the element.
	 */
	@NotNull
	List<ASTAnnotation> getInvisibleTypeAnnotations();

	/**
	 * @param annotations
	 * 		List of visible annotations to set on the element.
	 */
	void setVisibleAnnotations(@Nullable List<ASTAnnotation> annotations);

	/**
	 * @param annotations
	 * 		List of invisible annotations to set on the element.
	 */
	void setInvisibleAnnotations(@Nullable List<ASTAnnotation> annotations);

	/**
	 * @param annotation
	 * 		Visible annotation to add to the element.
	 */
	void addVisibleAnnotation(@NotNull ASTAnnotation annotation);

	/**
	 * @param annotation
	 * 		Invisible annotation to add to the element.
	 */
	void addInvisibleAnnotation(@NotNull ASTAnnotation annotation);

	/**
	 * @param annotations
	 * 		List of visible type annotations to set on the element.
	 */
	void setVisibleTypeAnnotations(@NotNull List<ASTAnnotation> annotations);

	/**
	 * @param annotations
	 * 		List of invisible type annotations to set on the element.
	 */
	void setInvisibleTypeAnnotations(@NotNull List<ASTAnnotation> annotations);

	/**
	 * @param annotation
	 * 		Visible type annotation to add to the element.
	 */
	void addVisibleTypeAnnotation(@NotNull ASTAnnotation annotation);

	/**
	 * @param annotation
	 * 		Invisible type annotation to add to the element.
	 */
	void addInvisibleTypeAnnotation(@NotNull ASTAnnotation annotation);
}
