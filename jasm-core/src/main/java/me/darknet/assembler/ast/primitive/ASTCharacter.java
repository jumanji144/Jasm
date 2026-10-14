package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

/**
 * Character literal node backed by a parser token.
 */
public class ASTCharacter extends ASTValue {
    public ASTCharacter(Token token) {
        super(ElementType.CHARACTER, token);
    }
}
