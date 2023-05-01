package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;

public class ASTValue extends ASTElement {

	public ASTValue(ElementType type, Token value) {
		super(type);
		this.value = value;
	}
}
