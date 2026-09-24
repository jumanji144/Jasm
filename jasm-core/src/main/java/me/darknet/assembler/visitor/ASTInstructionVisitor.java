package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.instructions.SemanticInstruction;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor interface for visiting source and semantic instructions.
 */
public interface ASTInstructionVisitor {

	/**
	 * Called on every instruction visit.
	 *
	 * @param instruction
	 * 		The semantic instruction, carrying its source node and typed operands.
	 */
	void visitInstruction(@NotNull SemanticInstruction instruction);

	/**
	 * Visit a label.
	 *
	 * @param label
	 * 		The label.
	 */
	void visitLabel(@NotNull ASTIdentifier label);

	/**
	 * Visit a line number.
	 *
	 * @param line
	 * 		The line number.
	 */
	void visitLineNumber(@NotNull ASTNumber line);

	/**
	 * Visit an exception handler.
	 *
	 * @param start
	 * 		The start label.
	 * @param end
	 * 		The end label.
	 * @param handler
	 * 		The handler label.
	 * @param type
	 * 		The exception type.
	 */
	void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler,
	                    @NotNull ASTIdentifier type);

	/**
	 * Called when the visitor is done visiting instructions.
	 */
	void visitEnd();
}
