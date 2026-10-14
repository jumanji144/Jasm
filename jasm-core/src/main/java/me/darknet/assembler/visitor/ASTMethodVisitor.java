package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.DiagnosticSink;
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
	 * Visits a declared exception type.
	 *
	 * @param exceptionType
	 * 		The exception type.
	 */
	default void visitDeclaredException(@NotNull ASTIdentifier exceptionType) {}

	/**
	 * Visits the default value of an annotation method.
	 *
	 * @param sink
	 * 		Sink to report problems to.
	 * @param defaultValue
	 * 		The default value.
	 */
	void visitAnnotationDefaultValue(@NotNull DiagnosticSink sink, ASTElement defaultValue);

	/**
	 * Visits the method body through the neutral instruction visitor protocol.
	 *
	 * @param sink
	 * 		Diagnostic sink to report errors to.
	 *
	 * @return Visitor for the method body, or {@code null} if the method body is not supported.
	 */
	default ASTInstructionVisitor visitCode(@NotNull DiagnosticSink sink) {
		return null;
	}

	/**
	 * Visits an annotation attached to one method parameter.
	 *
	 * @param visibility
	 * 		Visibility requested by the source keyword.
	 * @param index
	 * 		Source parameter index, including a receiver when present.
	 * @param classType
	 * 		The annotation type.
	 *
	 * @return Visitor for the annotation, or {@code null} if the annotation is not supported.
	 */
	default ASTAnnotationVisitor visitParameterAnnotation(@NotNull AnnotationVisibility visibility, int index,
	                                                      @NotNull ASTIdentifier classType) {
		throw new UnsupportedOperationException("Target does not support parameter annotations");
	}
}
