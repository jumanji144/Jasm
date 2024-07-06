package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;

import org.jetbrains.annotations.Nullable;

public interface ASTDeclarationVisitor extends ASTAnnotatedVisitor {

    void visitSignature(@Nullable ASTString signature);

    void visitEnd();

}
