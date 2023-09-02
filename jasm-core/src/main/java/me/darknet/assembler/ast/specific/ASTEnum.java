package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;

public class ASTEnum extends ASTElement {

    private final ASTIdentifier enumType;
    private final ASTIdentifier enumValue;

    public ASTEnum(ASTIdentifier type, ASTIdentifier value) {
        super(ElementType.ENUM, type, value);
        this.enumType = type;
        this.enumValue = value;
    }

    public ASTIdentifier enumType() {
        return enumType;
    }

    public ASTIdentifier enumValue() {
        return enumValue;
    }

}
