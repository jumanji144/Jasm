package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTField extends ASTMember {

    private final ASTIdentifier descriptor;
    private final @Nullable ASTValue value;

    public ASTField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor,
            @Nullable List<ASTAnnotation> annotations, @Nullable ASTIdentifier signature, @Nullable ASTValue value) {
        super(ElementType.FIELD, modifiers, name, signature, annotations);
        this.descriptor = descriptor;
        this.value = value;
    }

    public ASTIdentifier descriptor() {
        return descriptor;
    }

    public @Nullable ASTValue fieldValue() {
        return value;
    }

    public void accept(ErrorCollector collector, ASTFieldVisitor visitor) {
        super.accept(collector, visitor);
        if (visitor == null)
            return;
        if (value != null)
            visitor.visitValue(value);
        visitor.visitEnd();
    }
}
