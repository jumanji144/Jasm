package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;

public interface ASTAnnotationArrayVisitor {

    void visitValue(ASTValue value);

    void visitTypeValue(ASTIdentifier className);

    void visitEnumValue(ASTIdentifier className, ASTIdentifier enumName);

    ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier className);

    ASTAnnotationArrayVisitor visitArrayValue();

    void visitEnd();

}
