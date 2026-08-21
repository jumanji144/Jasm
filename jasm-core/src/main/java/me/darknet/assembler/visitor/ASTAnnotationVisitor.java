package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ASTAnnotationVisitor {
    void visitValue(ASTIdentifier name, ASTValue value);

    void visitTypeValue(ASTIdentifier name, ASTIdentifier className);

    void visitEnumValue(ASTIdentifier name, ASTIdentifier className, ASTIdentifier enumName);

    ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier name, ASTIdentifier className);

    ASTAnnotationArrayVisitor visitArrayValue(ASTIdentifier name);

    void visitEnd();

    static void accept(@NotNull ASTAnnotationVisitor visitor, @NotNull Collection<Pair<ASTIdentifier, ASTElement>> pairs, @NotNull ErrorCollector collector) {
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
                    ASTAnnotationArrayVisitor.accept(arrayVisitor, array, collector);
                }
                case ASTAnnotation annotation -> {
                    ASTAnnotationVisitor anno = visitor.visitAnnotationValue(key, annotation.classType());
                    annotation.accept(collector, anno);
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
                                    ElementMap<ASTIdentifier, ASTElement> map = new ElementMap<>();
                                    for (var subPair : annoObject.values().pairs())
                                        map.put(subPair.first(), subPair.second());
                                    ASTAnnotationVisitor.accept(anno, map.pairs(), collector);
                                    continue;
                                }
                            }
                        } catch (Exception ex) {
                            collector.addError("Unprocessable declaration (enum?) in annotation", key.location());
                            continue;
                        }
                    } else if (value == null) {
                        collector.addError("Unprocessable value in annotation", key.location());
                        continue;
                    }
                    collector.addError("Don't know how to process: " + value.type(), value.location());
                }
            }
        }

        visitor.visitEnd();
    }
}
