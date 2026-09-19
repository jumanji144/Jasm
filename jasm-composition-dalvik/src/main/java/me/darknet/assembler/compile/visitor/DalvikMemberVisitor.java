package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import me.darknet.dex.tree.definitions.Member;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationProcessing;
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
        return new DalvikAnnotationVisitor(DalvikAnnotationVisitor.RUNTIME, classType, this::acceptAnnotation);
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType) {
        return new DalvikAnnotationVisitor(DalvikAnnotationVisitor.BUILD, classType, this::acceptAnnotation);
    }

    @Override
    public ASTAnnotationVisitor visitSystemAnnotation(@NotNull ASTIdentifier classType) {
        return new DalvikAnnotationVisitor(DalvikAnnotationVisitor.SYSTEM, classType, this::acceptAnnotation);
    }

    @Override
    public ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        throw new IllegalStateException("Dalvik type annotations are not supported by the current dex tree");
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        throw new IllegalStateException("Dalvik type annotations are not supported by the current dex tree");
    }

    private void acceptAnnotation(Annotation annotation) {
        if (annotation.visibility() == Annotation.VISIBILITY_SYSTEM) {
            switch (AnnotationProcessing.processAttribute(java.util.Map.of(), member, annotation.annotation())) {
                case CONSUMED -> {
                    return;
                }
                case ERROR -> throw new IllegalStateException(
                        "Invalid member annotation: " + annotation.annotation().type().internalName()
                );
                case PRESERVE -> {
                }
            }
        }
        member.addAnnotation(annotation);
    }

}
