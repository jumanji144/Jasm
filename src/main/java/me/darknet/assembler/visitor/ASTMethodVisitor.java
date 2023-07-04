package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

public interface ASTMethodVisitor extends ASTDeclarationVisitor {

	void visitParameter(int index, ASTIdentifier name);

	ASTJvmInstructionVisitor visitJvmCode();

}
