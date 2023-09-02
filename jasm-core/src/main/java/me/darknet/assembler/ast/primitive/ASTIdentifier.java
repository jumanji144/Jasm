package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.EscapeUtil;

public class ASTIdentifier extends ASTLiteral {
    public ASTIdentifier(Token value) {
        super(ElementType.IDENTIFIER, value);
    }

    /**
     * @return the literal value of this identifier, aka escape sequences are
     *         unescaped
     */
    public String literal() {
        return EscapeUtil.unescape(content());
    }
}
