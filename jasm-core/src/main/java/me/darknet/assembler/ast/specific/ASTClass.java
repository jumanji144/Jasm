package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTClassVisitor;
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

    public @Nullable ASTIdentifier superName() {
        return superName;
    }

    public List<ASTIdentifier> interfaces() {
        return interfaces;
    }

    public List<ASTElement> contents() {
        return contents;
    }

    public @Nullable ASTElement content(int index) {
        return contents.get(index);
    }

    public void accept(ErrorCollector collector, ASTClassVisitor visitor) {
        super.accept(collector, visitor);
        if (visitor == null)
            return;

        visitor.visitSuperClass(superName);
        for (ASTIdentifier anInterface : interfaces) {
            visitor.visitInterface(anInterface);
        }

        for (ASTElement declaration : contents) {
            if (declaration instanceof ASTField field) {
                field.accept(collector, visitor.visitField(field.modifiers(), field.name(), field.descriptor()));
            } else if (declaration instanceof ASTMethod method) {
                method.accept(collector, visitor.visitMethod(method.modifiers(), method.name(), method.descriptor()));
            } else {
                collector.addError("Don't know how to process: " + declaration.type(), declaration.location());
            }
        }

        visitor.visitEnd();
    }
}
