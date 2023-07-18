package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

public interface ASTDeclarationVisitor {

	ASTAnnotationVisitor visitAnnotation(ASTIdentifier name);

	void visitSignature(ASTIdentifier signature);

	void visitEnd();

}
