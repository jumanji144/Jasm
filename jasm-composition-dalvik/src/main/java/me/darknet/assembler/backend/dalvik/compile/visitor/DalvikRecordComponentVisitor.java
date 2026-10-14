package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTRecordComponentVisitor;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Collects the annotations declared on one record component.
 *
 * @param annotations
 * 		The component's annotation list, appended to as annotations are visited.
 */
public record DalvikRecordComponentVisitor(@NotNull List<Annotation> annotations) implements ASTRecordComponentVisitor {
    @Override
    public ASTAnnotationVisitor visitAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType) {
        byte mapped = switch (visibility) {
            case VISIBLE -> DalvikAnnotationVisitor.RUNTIME;
            case INVISIBLE -> DalvikAnnotationVisitor.BUILD;
            case SYSTEM -> DalvikAnnotationVisitor.SYSTEM;
        };
        return new DalvikAnnotationVisitor(mapped, classType, annotations::add);
    }
}
