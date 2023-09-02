package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Pair;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;

public class ASTAnnotation extends ASTElement {

    private final ASTIdentifier classType;
    private final ElementMap<ASTIdentifier, ASTElement> values;

    public ASTAnnotation(ASTIdentifier classType, ElementMap<ASTIdentifier, ASTElement> values) {
        super(ElementType.ANNOTATION, CollectionUtil.merge(values.getElements(), classType));
        this.classType = classType;
        this.values = values;
    }

    public ASTIdentifier getClassType() {
        return classType;
    }

    public ElementMap<ASTIdentifier, ASTElement> getValues() {
        return values;
    }

    void accept(ErrorCollector collector, ASTAnnotationArrayVisitor visitor, ASTArray array) {
        // TODO: due to huge annotations, i would advice for a process queue.
        // But this is not a problem for now until xxDark notices this code, which i hope
        // he does not.
        for (ASTElement arrayValue : array.getValues()) {
            if(arrayValue instanceof ASTValue val) {
                visitor.visitValue(val);
            } else if(arrayValue instanceof ASTEnum astEnum) {
                visitor.visitEnumValue(astEnum.getEnumType(), astEnum.getEnumValue());
            } else if(arrayValue instanceof ASTAnnotation annotation) {
                ASTAnnotationVisitor anno = visitor.visitAnnotationValue(annotation.getClassType());
                annotation.accept(collector, anno);
            } else if(arrayValue instanceof ASTArray astArray) {
                ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue();
                accept(collector, arrayVisitor, astArray);
            } else if(arrayValue instanceof ASTEmpty) {
                ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue();
                accept(collector, arrayVisitor, ASTEmpty.EMPTY_ARRAY);
            } else {
                if(arrayValue == null) {
                    collector.addError("Unprocessable value in array", array.getLocation());
                    continue;
                }
                collector.addError("Don't know how to process: "
                        + arrayValue.getType(), arrayValue.getLocation());
            }
        }

        visitor.visitEnd();
    }

    public void accept(ErrorCollector collector, ASTAnnotationVisitor visitor) {
        for (Pair<ASTIdentifier, ASTElement> pair : values.getPairs()) {
            ASTElement value = pair.getSecond();
            if(value instanceof ASTValue val) {
                visitor.visitValue(pair.getFirst(), val);
            } else if(value instanceof ASTEnum astEnum) {
                visitor.visitEnumValue(pair.getFirst(), astEnum.getEnumType(), astEnum.getEnumValue());
            } else if(value instanceof ASTArray array) {
                ASTAnnotationArrayVisitor arrayVisitor = visitor.visitArrayValue(pair.getFirst());
                accept(collector, arrayVisitor, array);
            } else if(value instanceof ASTAnnotation annotation) {
                ASTAnnotationVisitor anno = visitor.visitAnnotationValue(pair.getFirst(), annotation.getClassType());
                annotation.accept(collector, anno);
            } else {
                if(value == null) {
                    collector.addError("Unprocessable value in annotation", getLocation());
                    continue;
                }
                collector.addError("Don't know how to process: "
                        + value.getType(), value.getLocation());
            }
        }

        visitor.visitEnd();
    }

}
