package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import me.darknet.dex.tree.definitions.Member;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationProcessing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles declaration attributes and annotations shared by Dalvik fields and methods.
 *
 * @param <T>
 * 		Dex member type populated by this visitor.
 */
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
        // no-op
    }

    @Override
    public ASTAnnotationVisitor visitAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType) {
        byte mapped = switch (visibility) {
            case VISIBLE -> DalvikAnnotationVisitor.RUNTIME;
            case INVISIBLE -> DalvikAnnotationVisitor.BUILD;
            case SYSTEM -> DalvikAnnotationVisitor.SYSTEM;
        };
        return new DalvikAnnotationVisitor(mapped, classType, this::acceptAnnotation);
    }

    private void acceptAnnotation(Annotation annotation) {
        if (annotation.visibility() == Annotation.VISIBILITY_SYSTEM) {
            switch (AnnotationProcessing.processAttribute(java.util.Map.of(), member, annotation.annotation())) {
                case CONSUMED -> {
                    return;
                }
                case ERROR -> throw new IllegalStateException("Invalid member annotation: " + annotation.annotation().type().internalName());
                case PRESERVE -> {}
            }
        }
        member.addAnnotation(annotation);
    }

}
