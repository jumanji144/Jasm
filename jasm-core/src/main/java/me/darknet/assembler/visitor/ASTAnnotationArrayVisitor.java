package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticSink;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor for annotation array values, including nested annotations, types, enums, and arrays.
 */
public interface ASTAnnotationArrayVisitor {
    /**
     * Visits a primitive value in an annotation array.
     *
     * @param value
     * 		Primitive annotation value.
     */
    void visitValue(ASTValue value);

    /**
     * Visits a class literal in an annotation array.
     *
     * @param className
     * 		Class identifier represented by the literal.
     */
    void visitTypeValue(ASTIdentifier className);

    /**
     * Visits an enum constant in an annotation array.
     *
     * @param className
     * 		Internal name of the enum class.
     * @param enumName
     * 		Name of the enum constant.
     */
    void visitEnumValue(ASTIdentifier className, ASTIdentifier enumName);

    /**
     * Begins a nested annotation value in an annotation array.
     *
     * @param className
     * 		Annotation type identifier.
     *
     * @return Visitor for the nested annotation, or {@code null} when it is ignored.
     */
    ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier className);

    /**
     * Begins a nested array value in an annotation array.
     *
     * @return Visitor for the nested array.
     */
    ASTAnnotationArrayVisitor visitArrayValue();

    /**
     * Signals that all values in this annotation array have been visited.
     *
     */
    void visitEnd();

    /**
     * Dispatches every value in an AST array to a visitor.
     *
     * @param visitor
     * 		Visitor receiving the array values.
     * @param array
     * 		Array to traverse.
     * @param sink
     * 		Diagnostic sink for unsupported values.
     */
    static void accept(@NotNull ASTAnnotationArrayVisitor visitor, @NotNull ASTArray array, @NotNull DiagnosticSink sink) {
        // TODO: due to huge annotations, i would advice for a process queue.
        //  But this is not a problem for now until xxDark notices this code, which i hope
        //  he does not.
        for (ASTElement arrayValue : array.values()) {
            switch (arrayValue) {
                case ASTValue val -> visitor.visitValue(val);
                case ASTIdentifier identifier -> visitor.visitTypeValue(identifier);
                case ASTEnum astEnum -> visitor.visitEnumValue(astEnum.enumOwner(), astEnum.enumFieldName());
                case ASTAnnotation annotation -> {
                    ASTAnnotationVisitor anno = visitor.visitAnnotationValue(annotation.getClassType());
                    if (anno != null)
                        ASTAnnotationVisitor.accept(anno, annotation.getValueMap().pairs(), sink);
                }
                case ASTArray astArray -> {
                    ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue();
                    accept(arrayVisitor, astArray, sink);
                }
                case ASTEmpty ignored -> {
                    ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue();
                    accept(arrayVisitor, ASTEmpty.EMPTY_ARRAY, sink);
                }
                case null, default -> {
                    if (arrayValue == null) {
                        sink.error(DiagnosticCode.UNSUPPORTED_FORM, "Unprocessable value in array", array.location());
                        continue;
                    }
                    sink.error(DiagnosticCode.UNSUPPORTED_FORM, "Don't know how to process: " + arrayValue.type(), arrayValue.location());
                }
            }
        }

        visitor.visitEnd();
    }
}
