package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

public class ASTBool extends ASTValue {

    public ASTBool(Token value) {
        super(ElementType.BOOL, value);
    }

    public boolean bool() {
        return Boolean.parseBoolean(value().content());
    }

}
