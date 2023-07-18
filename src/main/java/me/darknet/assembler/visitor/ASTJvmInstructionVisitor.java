package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;

public interface ASTJvmInstructionVisitor extends ASTInstructionVisitor {

	/**
	 * Indicates that the instruction should be a no arg instruction
	 */
	void visitInsn();

	void visitIntInsn(ASTNumber operand);

	void visitNewArrayInsn(ASTIdentifier type);

	void visitLdcInsn(ASTElement constant);

	void visitVarInsn(ASTIdentifier var);

	void visitIincInsn(ASTIdentifier var, ASTNumber increment);

	void visitJumpInsn(ASTIdentifier label);

	void visitTypeInsn(ASTIdentifier type);

	void visitLookupSwitchInsn(ASTObject lookupSwitchObject);

	void visitTableSwitchInsn(ASTObject tableSwitchObject);

	void visitFieldInsn(ASTIdentifier owner, ASTIdentifier name, ASTIdentifier descriptor);

	void visitMethodInsn(ASTIdentifier owner, ASTIdentifier name, ASTIdentifier descriptor);

	void visitInvokeDynamicInsn(ASTIdentifier name, ASTIdentifier descriptor, ASTArray bsm, ASTArray bsmArgs);

	void visitMultiANewArrayInsn(ASTIdentifier descriptor, ASTNumber numDimensions);

	void visitLabel(ASTIdentifier label);

	void visitLineNumber(ASTIdentifier label, ASTNumber line);

}
