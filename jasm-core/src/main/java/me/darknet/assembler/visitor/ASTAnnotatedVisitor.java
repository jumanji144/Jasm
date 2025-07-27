package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ASTAnnotatedVisitor {
    ASTAnnotationVisitor visitVisibleAnnotation(@NotNull ASTIdentifier classType);

    ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType);

    ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath);

    ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath);
}
