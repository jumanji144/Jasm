package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.util.CollectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ASTRecordComponent extends ASTElement implements ASTAnnotated, ASTSigned {
    private List<ASTAnnotation> visibleAnnotations = Collections.emptyList();
    private List<ASTAnnotation> invisibleAnnotations = Collections.emptyList();
    private final ASTIdentifier componentType;
    private final ASTIdentifier componentDescriptor;
    private ASTString signature;

    public ASTRecordComponent(@NotNull ASTIdentifier componentType, @NotNull ASTIdentifier componentDescriptor) {
        super(ElementType.RECORD_COMPONENT, CollectionUtil.fromVarArrayNonNull(componentType, componentDescriptor));
        this.componentType = componentType;
        this.componentDescriptor = componentDescriptor;
    }

    @NotNull
    public ASTIdentifier getComponentType() {
        return componentType;
    }

    @NotNull
    public ASTIdentifier getComponentDescriptor() {
        return componentDescriptor;
    }

    @Nullable
    public ASTString getSignature() {
        return signature;
    }

    @Override
    public void setSignature(@Nullable ASTString signature) {
        replaceChild(this.signature, signature);
        this.signature = signature;
    }

    @Override
    public @NotNull List<ASTAnnotation> getVisibleAnnotations() {
        return visibleAnnotations;
    }

    @Override
    public @NotNull List<ASTAnnotation> getInvisibleAnnotations() {
        return invisibleAnnotations;
    }

    @Override
    public void setVisibleAnnotations(@Nullable List<ASTAnnotation> annotations) {
        replaceChildren(this.visibleAnnotations, annotations);
        this.visibleAnnotations = annotations;
    }

    @Override
    public void setInvisibleAnnotations(@Nullable List<ASTAnnotation> annotations) {
        replaceChildren(this.invisibleAnnotations, annotations);
        this.invisibleAnnotations = annotations;
    }

    @Override
    public void addVisibleAnnotation(@NotNull ASTAnnotation annotation) {
        setVisibleAnnotations(CollectionUtil.merge(visibleAnnotations, annotation));
    }

    @Override
    public void addInvisibleAnnotation(@NotNull ASTAnnotation annotation) {
        setInvisibleAnnotations(CollectionUtil.merge(invisibleAnnotations, annotation));
    }
}
