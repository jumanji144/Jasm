package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTClass extends ASTMember {

    private final @Nullable ASTIdentifier superName;
    private final List<ASTIdentifier> interfaces;
    private final List<ASTElement> contents;

    public ASTClass(Modifiers modifiers, ASTIdentifier name, @Nullable ASTIdentifier signature,
            List<ASTAnnotation> annotations, @Nullable ASTIdentifier superName, List<ASTIdentifier> interfaces,
            List<ASTElement> contents) {
        super(ElementType.CLASS, modifiers, name, signature, annotations);
        this.children.addAll(contents);
        this.superName = superName;
        this.interfaces = interfaces;
        this.contents = contents;
    }

    public @Nullable ASTIdentifier getSuperName() {
        return superName;
    }

    public List<ASTIdentifier> getInterfaces() {
        return interfaces;
    }

    public List<ASTElement> getContents() {
        return contents;
    }

}
