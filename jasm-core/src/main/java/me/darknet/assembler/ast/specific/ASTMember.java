package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTMember extends ASTElement {

    private final Modifiers modifiers;
    private final ASTIdentifier name;
    private final @Nullable ASTIdentifier signature;
    private final List<ASTAnnotation> annotations;

    public ASTMember(ElementType type, Modifiers modifiers, ASTIdentifier name, @Nullable ASTIdentifier signature,
            List<ASTAnnotation> annotations) {
        super(type, CollectionUtil.merge(CollectionUtil.merge(modifiers.getModifiers(), annotations), name));
        if (modifiers.getModifiers().size() > 0) {
            this.value = modifiers.getModifiers().get(0).getValue();
        } else {
            this.value = name.getValue();
        }
        this.modifiers = modifiers;
        this.name = name;
        this.signature = signature;
        this.annotations = annotations;
    }

    public Modifiers getModifiers() {
        return modifiers;
    }

    public ASTIdentifier getName() {
        return name;
    }

    public @Nullable ASTIdentifier getSignature() {
        return signature;
    }

    public @Nullable List<ASTAnnotation> getAnnotations() {
        return annotations;
    }

}
