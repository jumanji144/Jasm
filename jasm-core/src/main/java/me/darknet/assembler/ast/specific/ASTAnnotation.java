package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ASTAnnotation extends ASTElement {

    private final ASTIdentifier classType;
    private final ElementMap<ASTIdentifier, ASTElement> values;
    private final ASTNumber typeRef;
    private final ASTIdentifier typePath;
    private final boolean visible;

    public ASTAnnotation(boolean visible, ASTIdentifier classType, ElementMap<ASTIdentifier, ASTElement> values) {
        this(visible, classType, values, null, null);
    }

    public ASTAnnotation(boolean visible, ASTIdentifier classType, ElementMap<ASTIdentifier, ASTElement> values, ASTNumber typeRef, ASTIdentifier typePath) {
        super(ElementType.ANNOTATION, CollectionUtil.mergeNonNull(values.elements(), classType));
        this.visible = visible;
        this.classType = classType;
        this.values = values;
        this.typeRef = typeRef;
        this.typePath = typePath;
    }

    public boolean isTypeAnnotation() {
        return typePath != null;
    }

    public boolean isVisible() {
        return visible;
    }

    public @Nullable ASTNumber typeRef() {
        return typeRef;
    }

    public @Nullable ASTIdentifier typePath() {
        return typePath;
    }

    public @NotNull ASTIdentifier classType() {
        return classType;
    }

    public @NotNull ElementMap<ASTIdentifier, ASTElement> values() {
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
