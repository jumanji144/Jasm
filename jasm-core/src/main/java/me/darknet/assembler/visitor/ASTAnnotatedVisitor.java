package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

public interface ASTAnnotatedVisitor {
    ASTAnnotationVisitor visitAnnotation(ASTIdentifier classType);
}
