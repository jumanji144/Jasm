package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Visitor for annotation element values, including nested annotations, arrays, types, and enums.
 */
public interface ASTAnnotationVisitor {
    /**
     * Visits a primitive annotation element value.
     *
     * @param name
     * 		Annotation element name.
     * @param value
     * 		Primitive annotation value.
     */
    void visitValue(ASTIdentifier name, ASTValue value);

    /**
     * Visits a class-literal annotation element value.
     *
     * @param name
     * 		Annotation element name.
     * @param className
     * 		Class identifier represented by the literal.
     */
    void visitTypeValue(ASTIdentifier name, ASTIdentifier className);

    /**
     * Visits an enum annotation element value.
     *
     * @param name
     * 		Annotation element name.
     * @param className
     * 		Internal name of the enum class.
     * @param enumName
     * 		Name of the enum constant.
     */
    void visitEnumValue(ASTIdentifier name, ASTIdentifier className, ASTIdentifier enumName);

    /**
     * Begins a nested annotation element value.
     *
     * @param name
     * 		Annotation element name.
     * @param className
     * 		Annotation type identifier.
     *
     * @return Visitor for the nested annotation, or {@code null} when it is ignored.
     */
    ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier name, ASTIdentifier className);

    /**
     * Begins an array-valued annotation element.
     *
     * @param name
     * 		Annotation element name.
     *
     * @return Visitor for the nested array.
     */
    ASTAnnotationArrayVisitor visitArrayValue(ASTIdentifier name);

    /**
     * Signals that all elements in this annotation have been visited.
     */
    void visitEnd();

    /**
     * Dispatches annotation element pairs to a visitor.
     *
     * @param visitor
     * 		Visitor receiving annotation elements.
     * @param pairs
     * 		Annotation element pairs to traverse.
     * @param sink
     * 		Diagnostic sink for unsupported values.
     */
    static void accept(@NotNull ASTAnnotationVisitor visitor, @NotNull Collection<Pair<ASTIdentifier, ASTElement>> pairs,
                       @NotNull DiagnosticSink sink) {
        for (Pair<ASTIdentifier, ASTElement> pair : pairs) {
            ASTElement value = pair.second();
            ASTIdentifier key = pair.first();
            switch (value) {
                case ASTValue val -> visitor.visitValue(key, val);
                case ASTIdentifier identifier -> visitor.visitTypeValue(key, identifier);
                case ASTEnum astEnum -> visitor.visitEnumValue(key, astEnum.enumOwner(), astEnum.enumFieldName());
                case ASTArray array -> {
                    ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue(key);
                    if (arrayVisitor == null) {
                        continue;
                    }
                    ASTAnnotationArrayVisitor.accept(arrayVisitor, array, sink);
                }
                case ASTAnnotation annotation -> {
                    ASTAnnotationVisitor anno = visitor.visitAnnotationValue(key, annotation.getClassType());
                    if (anno != null)
                        ASTAnnotationVisitor.accept(anno, annotation.getValueMap().pairs(), sink);
                }
                case null, default -> {
                    if (value instanceof ASTDeclaration declaration) {
                        try {
                            // Attempt to parse declaration as an enum/annotation
                            if (declaration.elements().size() == 2) {
                                String keyword = declaration.keyword().content();
                                if (keyword.equals(".enum")) {
                                    visitor.visitEnumValue(key, (ASTIdentifier) declaration.element(0), (ASTIdentifier) declaration.element(1));
                                    continue;
                                } else if (keyword.equals(".annotation")) {
                                    ASTIdentifier annoType = (ASTIdentifier) declaration.element(0);
                                    ASTObject annoObject = (ASTObject) declaration.element(1);
                                    ASTAnnotationVisitor anno = visitor.visitAnnotationValue(key, annoType);
                                    if (anno != null) {
                                        ElementMap<ASTIdentifier, ASTElement> map = new ElementMap<>();
                                        for (var subPair : annoObject.values().pairs())
                                            map.put(subPair.first(), subPair.second());
                                        ASTAnnotationVisitor.accept(anno, map.pairs(), sink);
                                    }
                                    continue;
                                }
                            }
                        } catch (Exception ex) {
                            sink.error(DiagnosticCode.UNSUPPORTED_FORM, "Unprocessable declaration (enum?) in annotation", key.location());
                            continue;
                        }
                    } else if (value == null) {
                        sink.error(DiagnosticCode.UNSUPPORTED_FORM, "Unprocessable value in annotation", key.location());
                        continue;
                    }
                    sink.error(DiagnosticCode.UNSUPPORTED_FORM, "Don't know how to process: " + value.type(), value.location());
                }
            }
        }

        visitor.visitEnd();
    }
}
