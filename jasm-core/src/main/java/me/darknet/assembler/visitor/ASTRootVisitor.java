package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.processing.ProcessedMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Root visitor for top-level annotations, classes, fields, and methods.
 */
public interface ASTRootVisitor {

    /**
     * Visits an annotation that stands on its own rather than inside a class, field, or method.
     *
     * @param annotation
     * 		Annotation declaration to visit.
     *
     * @return Visitor for the annotation, or {@code null} when the target cannot place standalone annotations.
     */
    @Nullable
    ASTAnnotationVisitor visitAnnotation(@NotNull ASTAnnotation annotation);

    /**
     * Begins visiting a top-level class declaration.
     *
     * @param modifiers
     * 		Class modifiers.
     * @param name
     * 		Class name.
     *
     * @return Visitor for the class declaration.
     */
    ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name);

    /**
     * Begins visiting a top-level field declaration.
     *
     * @param modifiers
     * 		Field modifiers.
     * @param name
     * 		Field name.
     * @param descriptor
     * 		Field descriptor.
     *
     * @return Visitor for the field declaration.
     */
    ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

    /**
     * Visits a top-level method with its target-neutral semantic view.
     *
     * @param method
     * 		Source method declaration.
     * @param processed
     * 		Validated semantic view paired with {@code method}.
     *
     * @return Visitor for the method.
     */
    ASTMethodVisitor visitMethod(@NotNull ASTMethod method, @NotNull ProcessedMethod processed);

}
