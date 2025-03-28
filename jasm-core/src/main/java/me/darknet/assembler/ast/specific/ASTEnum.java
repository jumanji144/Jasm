package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;

public class ASTEnum extends ASTElement {

    private final ASTIdentifier enumOwner;
    private final ASTIdentifier enumFieldName;
    private final ASTIdentifier enumFieldType;

    public ASTEnum(ASTIdentifier type, ASTIdentifier value) {
        super(ElementType.ENUM, type, value);
        this.enumOwner = type;
        this.enumFieldName = value;
        this.enumFieldType = null;
    }

    public ASTEnum(ASTIdentifier type, ASTIdentifier value, ASTIdentifier fieldType) {
        super(ElementType.ENUM, type, value, fieldType);
        this.enumOwner = type;
        this.enumFieldName = value;
        this.enumFieldType = fieldType;
    }

    public ASTIdentifier enumOwner() {
        return enumOwner;
    }

    public ASTIdentifier enumFieldName() {
        return enumFieldName;
    }

    public ASTIdentifier enumFieldType() {
        return enumFieldType;
    }

}
