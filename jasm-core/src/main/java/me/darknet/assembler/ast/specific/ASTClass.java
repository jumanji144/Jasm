package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.Modifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ASTClass extends ASTMember {
    private final @NotNull List<ASTElement> contents;
    private @NotNull List<ASTIdentifier> interfaces = Collections.emptyList();
    private @NotNull List<ASTInner> inners = Collections.emptyList();
    private @Nullable ASTIdentifier superName;
    private @Nullable ASTString sourceFile;

    public ASTClass(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull List<ASTElement> contents) {
        super(ElementType.CLASS, modifiers, name, name);
        this.children.addAll(contents);
        this.contents = contents;
    }

    @Nullable
    public ASTString getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(@Nullable ASTString sourceFile) {
        this.sourceFile = sourceFile;
    }

    public @Nullable ASTIdentifier getSuperName() {
        return superName;
    }

    public void setSuperName(@Nullable ASTIdentifier superName) {
        this.superName = superName;
    }

    @NotNull
    public List<ASTIdentifier> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(@NotNull List<ASTIdentifier> interfaces) {
        this.interfaces = interfaces;
    }

    @NotNull
    public List<ASTInner> getInners() {
        return inners;
    }

    public void setInnerClasses(@NotNull List<ASTInner> inners) {
        List<ASTInner> oldInners = this.inners;
        removeChildren(oldInners);
        addChildren(inners);
        this.inners = inners;
    }

    @NotNull
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

        visitor.visitSourceFile(sourceFile);
        visitor.visitSuperClass(superName);
        for (ASTIdentifier anInterface : interfaces) {
            visitor.visitInterface(anInterface);
        }

        for (ASTInner inner : inners) {
            visitor.visitInnerClass(inner.getModifiers(), inner.name(), inner.outerClass(), inner.innerClass());
        }

        for (ASTElement declaration : contents) {
            if (declaration instanceof ASTField field) {
                field.accept(
                        collector, visitor.visitField(field.getModifiers(), field.getName(), field.getDescriptor())
                );
            } else if (declaration instanceof ASTMethod method) {
                method.accept(
                        collector, visitor.visitMethod(method.getModifiers(), method.getName(), method.getDescriptor())
                );
            } else {
                collector.addError("Don't know how to process: " + declaration.type(), declaration.location());
            }
        }

        visitor.visitEnd();
    }
}
