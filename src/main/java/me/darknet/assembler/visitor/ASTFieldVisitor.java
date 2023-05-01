package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTLiteral;
import me.darknet.assembler.ast.specific.ASTValue;

public interface ASTFieldVisitor extends ASTDeclarationVisitor {

	void visitValue(ASTValue value);

}
