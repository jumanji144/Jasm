package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;

public class ASTIdentifier extends ASTLiteral {
	public ASTIdentifier(Token value) {
		super(ElementType.IDENTIFIER, value);
	}
}
