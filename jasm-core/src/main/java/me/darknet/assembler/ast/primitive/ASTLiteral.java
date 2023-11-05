package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;

public class ASTLiteral extends ASTElement {
    public ASTLiteral(ElementType type, Token value) {
        super(type);
        this.value = value;
    }

    @Override
    public String toString() {
        return content();
    }
}
