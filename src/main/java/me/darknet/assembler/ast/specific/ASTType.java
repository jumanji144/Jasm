package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;

public class ASTType extends ASTValue {

	public ASTType(ElementType type, Token value) {
		super(type, value);
	}

}
