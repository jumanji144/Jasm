package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import org.jetbrains.annotations.Nullable;

public interface ASTClassVisitor extends ASTDeclarationVisitor {

    void visitSuperClass(ASTIdentifier superClass);

    void visitInterface(ASTIdentifier interfaceName);

    void visitSourceFile(ASTString sourceFile);

    void visitInnerClass(Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
            ASTIdentifier innerClass);

    ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

    ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

}
