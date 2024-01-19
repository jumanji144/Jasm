package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import org.jetbrains.annotations.NotNull;

public interface ASTMethodVisitor extends ASTDeclarationVisitor {

    void visitParameter(int index, ASTIdentifier name);

    ASTJvmInstructionVisitor visitJvmCode(@NotNull ErrorCollector collector);

}
