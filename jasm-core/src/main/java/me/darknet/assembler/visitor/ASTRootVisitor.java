package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Root visitor for top-level annotations, classes, fields, and methods.
 */
public interface ASTRootVisitor {

    /**
     * Visits a top-level annotation.
     *
     * @param annotation
     *        The annotation and its source-syntax metadata.
     * @return Visitor for annotation elements, or {@code null} when unsupported.
     */
    @Nullable ASTAnnotationVisitor visitAnnotation(@NotNull ASTAnnotation annotation);

    ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name);

    ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

    ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

}
