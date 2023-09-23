package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import org.jetbrains.annotations.Nullable;

public interface ASTDeclarationVisitor {

    ASTAnnotationVisitor visitAnnotation(ASTIdentifier classType);

    void visitSignature(@Nullable ASTString signature);

    void visitEnd();

}
