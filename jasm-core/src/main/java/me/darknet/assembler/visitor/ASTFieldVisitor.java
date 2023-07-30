package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.specific.ASTValue;

public interface ASTFieldVisitor extends ASTDeclarationVisitor {

    void visitValue(ASTValue value);

}
