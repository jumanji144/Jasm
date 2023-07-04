package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;

public interface ASTAnnotationVisitor {

	void visitValue(ASTIdentifier name, ASTValue value);

	void visitEnumValue(ASTIdentifier name, ASTIdentifier className, ASTIdentifier enumName);

	ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier name, ASTIdentifier className);

	ASTAnnotationArrayVisitor visitArrayValue(ASTIdentifier name);

}
