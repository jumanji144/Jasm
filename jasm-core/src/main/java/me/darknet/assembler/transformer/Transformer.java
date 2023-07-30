package me.darknet.assembler.transformer;

import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.visitor.ASTRootVisitor;

import java.util.List;

public class Transformer {

    private final ASTRootVisitor visitor;

    public Transformer(ASTRootVisitor visitor) {
        this.visitor = visitor;
    }

    public void transform(List<ASTDeclaration> declarations) {

    }

}
