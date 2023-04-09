package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;

public class ASTString extends ASTElement {

	public ASTString(Token value) {
		super(ElementType.STRING);
		this.value = value;
	}

}
