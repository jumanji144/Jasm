package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import org.jetbrains.annotations.Nullable;

public class ASTAnnotation extends ASTElement {

    private final ASTIdentifier classType;
    private final ElementMap<ASTIdentifier, ASTElement> values;
    private final boolean visible;

    public ASTAnnotation(boolean visible, ASTIdentifier classType, ElementMap<ASTIdentifier, ASTElement> values) {
        super(ElementType.ANNOTATION, CollectionUtil.mergeNonNull(values.elements(), classType));
        this.visible = visible;
        this.classType = classType;
        this.values = values;
    }

    public boolean isVisible() {
        return visible;
    }

    public ASTIdentifier classType() {
        return classType;
    }

    public ElementMap<ASTIdentifier, ASTElement> values() {
        return values;
    }

    public <T extends ASTElement> T value(String name) {
        return values.get(name);
    }

    public void accept(ErrorCollector collector, @Nullable ASTAnnotationVisitor visitor) {
        if (visitor == null)
            return;
        ASTAnnotationVisitor.accept(visitor, values.pairs(), collector);
    }
}
