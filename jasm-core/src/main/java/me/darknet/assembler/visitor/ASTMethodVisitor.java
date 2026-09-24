package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor for method declarations.
 */
public interface ASTMethodVisitor extends ASTDeclarationVisitor {
	/**
	 * Visits a method parameter.
	 *
	 * @param index
	 * 		The parameter index.
	 * @param name
	 * 		The parameter name.
	 */
	void visitParameter(int index, @NotNull ASTIdentifier name);

	/**
	 * Visits the total Dalvik register count when it is explicitly declared.
	 *
	 * @param registers
	 * 		The total register count.
	 */
	default void visitRegisterCount(int registers) {}

	/**
	 * Visits a declared exception type.
	 *
	 * @param exceptionType
	 * 		The exception type.
	 */
	default void visitDeclaredException(@NotNull ASTIdentifier exceptionType) {}

	/**
	 * Visits the default value of an annotation method.
	 *
	 * @param defaultValue
	 * 		The default value.
	 */
	void visitAnnotationDefaultValue(ASTElement defaultValue);

	/**
	 * Visits the method body.
	 *
	 * @param collector
	 * 		Error collector to report errors to.
	 *
	 * @return Visitor for the method body, or {@code null} if the method body is not supported.
	 */
	default ASTJvmInstructionVisitor visitJvmCode(@NotNull ErrorCollector collector) {
		return null;
	}

	/**
	 * Visits the method body.
	 *
	 * @param collector
	 * 		Error collector to report errors to.
	 *
	 * @return Visitor for the method body, or {@code null} if the method body is not supported.
	 */
	default ASTDalvikInstructionVisitor visitDalvikCode(@NotNull ErrorCollector collector) {
		return null;
	}

	/**
	 * Visits a parameter annotation with its source-syntax visibility.
	 *
	 * @param visibility
	 * 		Visibility represented by the annotation's source syntax.
	 * @param index
	 * 		The zero-based JVM parameter index emitted by the current AST traversal; an instance receiver is excluded.
	 * @param classType
	 * 		The annotation type.
	 *
	 * @return Visitor for the annotation.
	 * @throws UnsupportedOperationException if parameter annotations are unsupported.
	 */
	default ASTAnnotationVisitor visitParameterAnnotation(@NotNull AnnotationVisibility visibility, int index,
	                                                       @NotNull ASTIdentifier classType) {
		throw new UnsupportedOperationException("Target does not support parameter annotations");
	}
}
