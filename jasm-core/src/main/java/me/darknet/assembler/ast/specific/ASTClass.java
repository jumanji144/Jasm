package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.ASTRecordComponentVisitor;
import me.darknet.assembler.visitor.Modifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ASTClass extends ASTMember {
    private final @NotNull List<ASTElement> contents;
    private @NotNull List<ASTIdentifier> interfaces = Collections.emptyList();
    private @NotNull List<ASTIdentifier> permittedSubclasses = Collections.emptyList();
    private @NotNull List<ASTRecordComponent> recordComponents = Collections.emptyList();
    private @NotNull List<ASTInner> inners = Collections.emptyList();
    private @Nullable ASTIdentifier superName;
    private @Nullable ASTString sourceFile;
    private @Nullable ASTElement outerClass;
    private @Nullable ASTOuterMethod outerMethod;
    private @Nullable ASTIdentifier nestHost;
    private @NotNull List<ASTIdentifier> nestMembers = Collections.emptyList();

    public ASTClass(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull List<ASTElement> contents) {
        super(ElementType.CLASS, modifiers, name, name);
        addChildren(contents);
        this.contents = contents;
    }

    @Nullable
    public ASTString getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(@Nullable ASTString sourceFile) {
        replaceChild(this.sourceFile, sourceFile);
        this.sourceFile = sourceFile;
    }

    public @Nullable ASTIdentifier getSuperName() {
        return superName;
    }

    public void setSuperName(@Nullable ASTIdentifier superName) {
        replaceChild(this.superName, superName);
        this.superName = superName;
    }

    @Nullable
    public ASTElement getOuterClass() {
        return outerClass;
    }

    public void setOuterClass(@Nullable ASTElement outerClass) {
        replaceChild(this.outerClass, outerClass);
        this.outerClass = outerClass;
    }

    @Nullable
    public ASTOuterMethod getOuterMethod() {
        return outerMethod;
    }

    public void setOuterMethod(@Nullable ASTOuterMethod outerMethod) {
        replaceChild(this.outerMethod, outerMethod);
        this.outerMethod = outerMethod;
    }

    public void setNestHost(@Nullable ASTIdentifier nestHost) {
        replaceChild(this.nestHost, nestHost);
        this.nestHost = nestHost;
    }

    public void setNestMembers(@NotNull List<ASTIdentifier> nestMembers) {
        replaceChildren(this.nestMembers, nestMembers);
        this.nestMembers = nestMembers;
    }

    @NotNull
    public List<ASTIdentifier> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(@NotNull List<ASTIdentifier> interfaces) {
        replaceChildren(this.interfaces, interfaces);
        this.interfaces = interfaces;
    }

    public void setPermittedSubclasses(@NotNull List<ASTIdentifier> permittedSubclasses) {
        this.permittedSubclasses = permittedSubclasses;
    }

    @NotNull
    public List<ASTRecordComponent> getRecordComponents() {
        return recordComponents;
    }

    public void setRecordComponents(@NotNull List<ASTRecordComponent> recordComponents) {
        this.recordComponents = recordComponents;
    }

    @NotNull
    public List<ASTIdentifier> getPermittedSubclasses() {
        return permittedSubclasses;
    }

    @NotNull
    public List<ASTInner> getInners() {
        return inners;
    }

    public void setInnerClasses(@NotNull List<ASTInner> inners) {
        replaceChildren(this.inners, inners);
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
        visitor.visitOuterClass(outerClass);
        visitor.visitOuterMethod(outerMethod);
        visitor.visitNestHost(nestHost);
        for (ASTIdentifier nestMember : nestMembers) {
            visitor.visitNestMember(nestMember);
        }
        for (ASTIdentifier anInterface : interfaces) {
            visitor.visitInterface(anInterface);
        }

        for (ASTIdentifier permittedSubclass : permittedSubclasses) {
            visitor.visitPermittedSubclass(permittedSubclass);
        }

        for (ASTRecordComponent recordComponent : recordComponents) {
            ASTRecordComponentVisitor componentVisitor = visitor.visitRecordComponent(recordComponent.getComponentType(),
                    recordComponent.getComponentDescriptor(), recordComponent.getSignature());
            for (ASTAnnotation annotation : recordComponent.getVisibleAnnotations()) {
                annotation.accept(collector, componentVisitor.visitVisibleAnnotation(annotation.classType()));
            }
            for (ASTAnnotation annotation : recordComponent.getInvisibleAnnotations()) {
                annotation.accept(collector, componentVisitor.visitInvisibleAnnotation(annotation.classType()));
            }
            for (ASTAnnotation annotation : recordComponent.getVisibleTypeAnnotations()) {
                annotation.accept(collector, componentVisitor.visitVisibleTypeAnnotation(annotation.classType(), annotation.typeRef(), annotation.typePath()));
            }
            for (ASTAnnotation annotation : recordComponent.getInvisibleTypeAnnotations()) {
                annotation.accept(collector, componentVisitor.visitInvisibleTypeAnnotation(annotation.classType(), annotation.typeRef(), annotation.typePath()));
            }
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
