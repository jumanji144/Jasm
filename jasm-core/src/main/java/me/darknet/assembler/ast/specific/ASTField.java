package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.Modifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ASTField extends ASTMember {
    private final @Nullable ASTValue value;

    public ASTField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor,
            @Nullable ASTValue value) {
        super(ElementType.FIELD, modifiers, name, descriptor);
        this.value = value;
    }

    public @Nullable ASTValue getFieldValue() {
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
