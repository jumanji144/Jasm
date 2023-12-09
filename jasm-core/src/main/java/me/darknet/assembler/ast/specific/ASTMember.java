package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import me.darknet.assembler.visitor.Modifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ASTMember extends ASTElement implements ASTSigned, ASTAccessed, ASTAnnotated {
    private final @NotNull ASTIdentifier name;
    private final @NotNull ASTIdentifier descriptor;
    private final @NotNull Modifiers modifiers;
    private @Nullable ASTString signature;
    private List<ASTAnnotation> annotations = Collections.emptyList();

    public ASTMember(@NotNull ElementType type, @NotNull Modifiers modifiers, @NotNull ASTIdentifier name,
            @NotNull ASTIdentifier descriptor) {
        super(type, CollectionUtil.merge(modifiers.modifiers(), name));
        if (!modifiers.modifiers().isEmpty()) {
            this.value = modifiers.modifiers().get(0).value();
        } else {
            this.value = name.value();
        }
        this.modifiers = modifiers;
        this.name = name;
        this.descriptor = descriptor;
    }

    @NotNull
    public ASTIdentifier getName() {
        return name;
    }

    @NotNull
    public ASTIdentifier getDescriptor() {
        return descriptor;
    }

    @Override
    public @NotNull Modifiers getModifiers() {
        return modifiers;
    }

    @Override
    public @Nullable ASTString getSignature() {
        return signature;
    }

    @Override
    public void setSignature(@Nullable ASTString signature) {
        this.signature = signature;
    }

    @Override
    public @NotNull List<ASTAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(@Nullable List<ASTAnnotation> annotations) {
        List<ASTAnnotation> oldAnnotations = this.annotations;
        removeChildren(oldAnnotations);
        if (annotations != null)
            addChildren(annotations);
        this.annotations = annotations;
    }

    @Override
    public void addAnnotation(@NotNull ASTAnnotation annotation) {
        setAnnotations(CollectionUtil.merge(annotations, annotation));
    }

    protected void accept(ErrorCollector collector, ASTDeclarationVisitor visitor) {
        if (visitor == null) {
            collector.addError("Unable to process member", null);
            return;
        }
        for (ASTAnnotation annotation : annotations) {
            annotation.accept(collector, visitor.visitAnnotation(annotation.classType()));
        }
        if (signature != null)
            visitor.visitSignature(signature);
    }
}
