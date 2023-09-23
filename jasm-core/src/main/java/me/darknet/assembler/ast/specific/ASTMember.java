package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTMember extends ASTElement {

    private final Modifiers modifiers;
    private final ASTIdentifier name;
    private final @Nullable ASTString signature;
    private final List<ASTAnnotation> annotations;

    public ASTMember(ElementType type, Modifiers modifiers, ASTIdentifier name, @Nullable ASTString signature,
                     List<ASTAnnotation> annotations) {
        super(type, CollectionUtil.merge(CollectionUtil.merge(modifiers.modifiers(), annotations), name));
        if (!modifiers.modifiers().isEmpty()) {
            this.value = modifiers.modifiers().get(0).value();
        } else {
            this.value = name.value();
        }
        this.modifiers = modifiers;
        this.name = name;
        this.signature = signature;
        this.annotations = annotations;
    }

    public Modifiers modifiers() {
        return modifiers;
    }

    public ASTIdentifier name() {
        return name;
    }

    public @Nullable ASTString signature() {
        return signature;
    }

    public @Nullable List<ASTAnnotation> annotations() {
        return annotations;
    }

    protected void accept(ErrorCollector collector, ASTDeclarationVisitor visitor) {
        if (visitor == null) {
            collector.addError("Unable to process member", null);
            return;
        }
        for (ASTAnnotation annotation : annotations) {
            annotation.accept(collector, visitor.visitAnnotation(annotation.classType()));
        }
        if(signature != null) visitor.visitSignature(signature);
    }

}
