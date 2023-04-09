package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;

public class ASTIdentifier extends ASTElement {
	public ASTIdentifier(Token value) {
		super(ElementType.IDENTIFIER);
		this.value = value;
	}
}
