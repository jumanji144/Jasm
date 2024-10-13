package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.error.ErrorCollector;
import org.jetbrains.annotations.NotNull;

public interface ASTAnnotationArrayVisitor {
    void visitValue(ASTValue value);

    void visitTypeValue(ASTIdentifier className);

    void visitEnumValue(ASTIdentifier className, ASTIdentifier enumName);

    ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier className);

    ASTAnnotationArrayVisitor visitArrayValue();

    void visitEnd();

    static void accept(@NotNull ASTAnnotationArrayVisitor visitor, @NotNull ASTArray array, @NotNull ErrorCollector collector) {
        // TODO: due to huge annotations, i would advice for a process queue.
        //  But this is not a problem for now until xxDark notices this code, which i hope
        //  he does not.
        for (ASTElement arrayValue : array.values()) {
            if (arrayValue instanceof ASTValue val) {
                visitor.visitValue(val);
            } else if (arrayValue instanceof ASTIdentifier identifier) {
                visitor.visitTypeValue(identifier);
            } else if (arrayValue instanceof ASTEnum astEnum) {
                visitor.visitEnumValue(astEnum.enumType(), astEnum.enumValue());
            } else if (arrayValue instanceof ASTAnnotation annotation) {
                ASTAnnotationVisitor anno = visitor.visitAnnotationValue(annotation.classType());
                annotation.accept(collector, anno);
            } else if (arrayValue instanceof ASTArray astArray) {
                ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue();
                accept(arrayVisitor, astArray, collector);
            } else if (arrayValue instanceof ASTEmpty) {
                ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue();
                accept(arrayVisitor, ASTEmpty.EMPTY_ARRAY, collector);
            } else {
                if (arrayValue == null) {
                    collector.addError("Unprocessable value in array", array.location());
                    continue;
                }
                collector.addError("Don't know how to process: " + arrayValue.type(), arrayValue.location());
            }
        }

        visitor.visitEnd();
    }
}
