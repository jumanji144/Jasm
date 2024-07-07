package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;

import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ASTClassVisitor extends ASTDeclarationVisitor {

    void visitSuperClass(@Nullable ASTIdentifier superClass);

    void visitInterface(@NotNull ASTIdentifier interfaceName);

    void visitSourceFile(@Nullable ASTString sourceFile);
    void visitOuterClass(@Nullable ASTElement outerClass);
    void visitOuterMethod(@Nullable ASTOuterMethod outerMethod);

    void visitPermittedSubclass(@NotNull ASTIdentifier subclass);

    ASTRecordComponentVisitor visitRecordComponent(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor, @Nullable ASTString signature);

    void visitInnerClass(@NotNull Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
                         @Nullable ASTIdentifier innerClass);

    ASTFieldVisitor visitField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor);

    ASTMethodVisitor visitMethod(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name,@NotNull  ASTIdentifier descriptor);

}
