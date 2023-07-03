package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

public class ASTBool extends ASTValue {

	public ASTBool(Token value) {
		super(ElementType.BOOL, value);
	}

	public boolean getBool() {
		return Boolean.parseBoolean(getValue().getContent());
	}

}
