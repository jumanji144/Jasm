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
    private List<ASTAnnotation> visibleAnnotations = Collections.emptyList();
    private List<ASTAnnotation> invisibleAnnotations = Collections.emptyList();

    public ASTMember(@NotNull ElementType type, @NotNull Modifiers modifiers, @NotNull ASTIdentifier name,
            @NotNull ASTIdentifier descriptor) {
        super(type, CollectionUtil.merge(modifiers.modifiers(), name));
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

    protected void accept(ErrorCollector collector, ASTDeclarationVisitor visitor) {
        if (visitor == null) {
            collector.addError("Unable to process member", null);
            return;
        }
	    for (ASTAnnotation annotation : visibleAnnotations)
		    annotation.accept(collector, visitor.visitVisibleAnnotation(annotation.classType()));
	    for (ASTAnnotation annotation : invisibleAnnotations)
		    annotation.accept(collector, visitor.visitInvisibleAnnotation(annotation.classType()));
        if (signature != null)
            visitor.visitSignature(signature);
    }
}
