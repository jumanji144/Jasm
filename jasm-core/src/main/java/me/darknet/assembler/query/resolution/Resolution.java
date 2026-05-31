package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import org.jetbrains.annotations.NotNull;

/**
 * A resolution of an AST element.
 */
public sealed interface Resolution permits EmptyResolution, ClassResolution, FieldResolution, MethodResolution,
		ClassAnnotationResolution, FieldAnnotationResolution, MethodAnnotationResolution,
		IndependentAnnotationResolution, ClassExtends, ClassImplements, InnerClassResolution,
		InstructionResolution, LabelDeclarationResolution, LabelReferenceResolution,
		VariableDeclarationResolution, VariableReferenceResolution, TypeReferenceResolution {
	/**
	 * @return The AST element that was resolved.
	 */
	@NotNull ASTElement element();

	/**
	 * @return {@code true} if this resolution is empty, IE it does not resolve to any element.
	 */
	default boolean isEmpty() {
		return false;
	}
}
