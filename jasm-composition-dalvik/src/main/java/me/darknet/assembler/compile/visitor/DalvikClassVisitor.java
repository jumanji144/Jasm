package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.DalvikModifiers;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.visitor.*;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DalvikClassVisitor(ClassDefinition definition) implements ASTClassVisitor {
    @Override
    public void visitSuperClass(@Nullable ASTIdentifier superClass) {
        definition.setSuperClass(null);
    }

    @Override
    public void visitInterface(@NotNull ASTIdentifier interfaceName) {
        definition.addInterface(Types.instanceTypeFromInternalName(interfaceName.literal()));
    }

    @Override
    public void visitSourceFile(@Nullable ASTString sourceFile) {
        definition.setSourceFile(sourceFile == null ? null : sourceFile.content());
    }

    @Override
    public void visitOuterClass(@Nullable ASTElement outerClass) {

    }

    @Override
    public void visitOuterMethod(@Nullable ASTOuterMethod outerMethod) {

    }

    @Override
    public void visitPermittedSubclass(@NotNull ASTIdentifier subclass) {

    }

    @Override
    public void visitNestHost(@Nullable ASTIdentifier nestHost) {

    }

    @Override
    public void visitNestMember(@NotNull ASTIdentifier nestMember) {

    }

    @Override
    public ASTRecordComponentVisitor visitRecordComponent(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor, @Nullable ASTString signature) {
        return null;
    }

    @Override
    public void visitInnerClass(@NotNull Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass, @Nullable ASTIdentifier innerClass) {

    }

    @Override
    public ASTFieldVisitor visitField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor) {
        ClassType type = new TypeParser(descriptor.literal()).requireClassType();
        FieldMember member = new FieldMember(name.literal(), type, DalvikModifiers.getFieldModifiers(modifiers));
        definition.putField(member);
        return new DalvikFieldVisitor(member);
    }

    @Override
    public ASTMethodVisitor visitMethod(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor) {
        return null;
    }

    @Override
    public void visitSignature(@Nullable ASTString signature) {

    }

    @Override
    public void visitEnd() {

    }

    @Override
    public ASTAnnotationVisitor visitVisibleAnnotation(ASTIdentifier classType) {
        return null;
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleAnnotation(ASTIdentifier classType) {
        return null;
    }

    @Override
    public ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        return null;
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        return null;
    }
}
