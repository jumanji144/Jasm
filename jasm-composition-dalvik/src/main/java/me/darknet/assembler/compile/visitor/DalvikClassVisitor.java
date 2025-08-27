package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.visitor.*;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DalvikClassVisitor(ClassDefinition definition) implements ASTClassVisitor {
    @Override
    public void visitSuperClass(@Nullable ASTIdentifier superClass) {
        definition.setSuperClass(null);
    }

    @Override
    public void visitInterface(@NotNull ASTIdentifier interfaceName) {

    }

    @Override
    public void visitSourceFile(@Nullable ASTString sourceFile) {

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
        return null;
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
}
