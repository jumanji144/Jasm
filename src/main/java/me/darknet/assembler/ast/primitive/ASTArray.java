package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;

import java.util.List;

public class ASTArray extends ASTElement {

	private final List<ASTElement> values;

	public ASTArray(List<ASTElement> values) {
		super(ElementType.ARRAY, values);
		this.values = values;
	}

	public List<ASTElement> getValues() {
		return values;
	}

}
