package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.ElementMapView;
import me.darknet.assembler.util.ImmutableElementMap;

import org.jetbrains.annotations.Nullable;

public class ASTObject extends ASTElement {

    private final ImmutableElementMap<ASTIdentifier, @Nullable ASTElement> values;

    public ASTObject(ElementMapView<ASTIdentifier, @Nullable ASTElement> values) {
        this(ImmutableElementMap.copyOf(values));
    }

    private ASTObject(ImmutableElementMap<ASTIdentifier, @Nullable ASTElement> values) {
        super(ElementType.OBJECT, values.elements());
        if (values.size() != 0) {
            this.value = values.get(0).value();
        } else {
            this.value = null;
        }
        this.values = values;
    }

    public ElementMapView<ASTIdentifier, ASTElement> values() {
        return values;
    }

    public <T extends @Nullable ASTElement> T value(String name) {
        return values.get(name);
    }
}
