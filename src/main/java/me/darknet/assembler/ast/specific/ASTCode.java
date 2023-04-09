package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;

import java.util.List;

public class ASTCode extends ASTElement {

	private final List<ASTElement> instructions;

	public ASTCode(List<ASTElement> instructions) {
		super(ElementType.CODE, instructions);
		this.instructions = instructions;
	}

	public List<ASTElement> getInstructions() {
		return instructions;
	}

}
