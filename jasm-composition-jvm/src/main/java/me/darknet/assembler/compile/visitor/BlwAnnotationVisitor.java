package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.annotation.AnnotationBuilder;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;

public class BlwAnnotationVisitor implements ASTAnnotationVisitor {
    private final AnnotationBuilder<?> builder;

    public BlwAnnotationVisitor(AnnotationBuilder<?> builder) {
        this.builder = builder;
    }

    @Override
    public void visitValue(ASTIdentifier name, ASTValue value) {

    }

    @Override
    public void visitEnumValue(ASTIdentifier name, ASTIdentifier className, ASTIdentifier enumName) {

    }

    @Override
    public ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier name, ASTIdentifier className) {
        return null;
    }

    @Override
    public ASTAnnotationArrayVisitor visitArrayValue(ASTIdentifier name) {
        return null;
    }

    @Override
    public void visitEnd() {

    }
}
