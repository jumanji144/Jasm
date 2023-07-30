package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

public class ASTInner extends ASTElement {

    private final @Nullable ASTIdentifier name;
    private final @Nullable ASTIdentifier outerClass;
    private final ASTIdentifier innerClass;
    private final Modifiers modifiers;

    public ASTInner(Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
            ASTIdentifier innerClass) {
        super(ElementType.INNER_CLASS, CollectionUtil.merge(modifiers.getModifiers(), name, innerClass, outerClass));
        this.modifiers = modifiers;
        this.name = name;
        this.outerClass = outerClass;
        this.innerClass = innerClass;
    }

    public Modifiers getModifiers() {
        return modifiers;
    }

    public @Nullable ASTIdentifier getName() {
        return name;
    }

    public @Nullable ASTIdentifier getOuterClass() {
        return outerClass;
    }

    public ASTIdentifier getInnerClass() {
        return innerClass;
    }

}
