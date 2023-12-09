package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ASTInner extends ASTElement implements ASTAccessed {

    private final @Nullable ASTIdentifier name;
    private final @Nullable ASTIdentifier outerClass;
    private final ASTIdentifier innerClass;
    private final Modifiers modifiers;

    public ASTInner(Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
            ASTIdentifier innerClass) {
        super(
                ElementType.INNER_CLASS,
                CollectionUtil.mergeNonNull(modifiers.modifiers(), name, innerClass, outerClass)
        );
        this.modifiers = modifiers;
        this.name = name;
        this.outerClass = outerClass;
        this.innerClass = innerClass;
    }

    @Override
    public @NotNull Modifiers getModifiers() {
        return modifiers;
    }

    public @Nullable ASTIdentifier name() {
        return name;
    }

    public @Nullable ASTIdentifier outerClass() {
        return outerClass;
    }

    public ASTIdentifier innerClass() {
        return innerClass;
    }

}
