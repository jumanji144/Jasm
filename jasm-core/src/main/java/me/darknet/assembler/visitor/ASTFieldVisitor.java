package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;

public interface ASTFieldVisitor extends ASTDeclarationVisitor {

    void visitValue(ASTElement value);

}
