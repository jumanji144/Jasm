package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import org.jetbrains.annotations.NotNull;

public interface ASTMethodVisitor extends ASTDeclarationVisitor {

    void visitParameter(int index, ASTIdentifier name);

    void visitAnnotationDefaultValue(ASTElement defaultValue);

    default ASTJvmInstructionVisitor visitJvmCode(@NotNull ErrorCollector collector) {
        return null;
    }

    default ASTDalvikInstructionVisitor visitDalvikCode(@NotNull ErrorCollector collector) {
        return null;
    }

    ASTAnnotationVisitor visitVisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType);

    ASTAnnotationVisitor visitInvisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType);
}
