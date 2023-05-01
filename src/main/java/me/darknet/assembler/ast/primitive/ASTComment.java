package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.parser.Token;

public class ASTComment extends ASTElement {

	public ASTComment(Token comment) {
		super(ElementType.COMMENT);
		this.value = comment;
	}

}
