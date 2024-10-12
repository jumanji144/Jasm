package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

public interface ASTAnnotatedVisitor {
    ASTAnnotationVisitor visitVisibleAnnotation(ASTIdentifier classType);
    ASTAnnotationVisitor visitInvisibleAnnotation(ASTIdentifier classType);
}
