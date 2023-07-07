package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.instructions.Constant;
import me.darknet.assembler.instructions.Handle;

import java.util.List;

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

	void visitLookupSwitchInsn(String defaultLabel, List<String> labels, List<Integer> keys);

	void visitTableSwitchInsn(String defaultLabel, List<String> labels, int min, int max);

	void visitFieldInsn(String owner, String name, String descriptor);

	void visitMethodInsn(String owner, String name, String descriptor, boolean itf);

	void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, List<Constant> bsmArgs);

	void visitMultiANewArrayInsn(String descriptor, int numDimensions);

}
