package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTString;
import org.jetbrains.annotations.Nullable;

/**
 * Visitor for AST declarations, such as classes, methods, and fields.
 */
public interface ASTDeclarationVisitor extends ASTAnnotatedVisitor {
	/**
	 * Visits the signature of the declaration, if present.
	 *
	 * @param signature
	 * 		The signature of the declaration, or {@code null} if not present.
	 */
	void visitSignature(@Nullable ASTString signature);

	/**
	 * Visits the deprecated attribute of the declaration, if present.
	 */
	default void visitDeprecated() {}

	/**
	 * Visits the end of the declaration. This method is called after all other {@code visit} methods have been called.
	 */
	void visitEnd();
}
