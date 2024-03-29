package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.ElementMap;

import org.jetbrains.annotations.Nullable;

public class ASTObject extends ASTElement {

    private final ElementMap<ASTIdentifier, @Nullable ASTElement> values;

    public ASTObject(ElementMap<ASTIdentifier, @Nullable ASTElement> values) {
        super(ElementType.OBJECT, values.elements());
        if (values.size() != 0) {
            this.value = values.get(0).value();
        } else {
            this.value = null;
        }
        this.values = values;
    }

    public ElementMap<ASTIdentifier, ASTElement> values() {
        return values;
    }

    public <T extends @Nullable ASTElement> T value(String name) {
        return values.get(name);
    }
}
