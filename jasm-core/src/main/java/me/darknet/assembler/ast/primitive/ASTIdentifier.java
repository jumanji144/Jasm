package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.util.EscapeUtil;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

public class ASTIdentifier extends ASTLiteral {
	public static final ASTIdentifier STUB = new ASTIdentifier(new Token(Range.EMPTY, Location.UNKNOWN, TokenType.IDENTIFIER, ""));

	public ASTIdentifier(Token value) {
        super(ElementType.IDENTIFIER, value);
    }

    /**
     * @return The literal value of this identifier, aka escape sequences are
     *         unescaped
     *
     * @see #content() Content without escape handling.
     */
    public String literal() {
        return EscapeUtil.unescape(content());
    }
}
