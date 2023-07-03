package me.darknet.assembler.ast.primitive;

import java.util.Collections;

public class ASTLabel extends ASTInstruction {

	public ASTLabel(ASTIdentifier identifier) {
		super(identifier, Collections.emptyList());
	}

}
