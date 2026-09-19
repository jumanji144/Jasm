package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor interface for visiting instructions in an AST.
 */
public interface ASTInstructionVisitor {

	/**
	 * Called on every instruction visit.
	 *
	 * @param instruction
	 * 		The instruction
	 */
	void visitInstruction(@NotNull ASTInstruction instruction);

	/**
	 * Visit a label
	 *
	 * @param label
	 * 		the label
	 */
	void visitLabel(@NotNull ASTIdentifier label);

	/**
	 * Visit a line number
	 *
	 * @param line
	 * 		the line number
	 */
	void visitLineNumber(@NotNull ASTNumber line);

	/**
	 * Visit an exception handler
	 *
	 * @param start
	 * 		the start label
	 * @param end
	 * 		the end label
	 * @param handler
	 * 		the handler label
	 * @param type
	 * 		the exception type
	 */
	void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type);

	/**
	 * Called when the visitor is done visiting instructions.
	 */
	void visitEnd();
}
