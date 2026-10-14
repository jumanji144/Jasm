package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

/**
 * String literal node backed by a parser token.
 */
public class ASTString extends ASTValue {

    public ASTString(Token value) {
        super(ElementType.STRING, value);
    }

}
