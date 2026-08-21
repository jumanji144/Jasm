package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import me.darknet.dex.tree.definitions.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DalvikMemberVisitor<T extends Member<?>> implements ASTDeclarationVisitor {

    protected final T member;

    public DalvikMemberVisitor(T member) {
        this.member = member;
    }

    @Override
    public void visitSignature(@Nullable ASTString signature) {
        member.setSignature(signature == null ? null : signature.content());
    }

    @Override
    public void visitEnd() {

    }

    @Override
    public ASTAnnotationVisitor visitVisibleAnnotation(@NotNull ASTIdentifier classType) {
        return null;
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType) {
        return null;
    }

    @Override
    public ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        return null;
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        return null;
    }

}
