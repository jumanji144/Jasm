package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTCode extends ASTElement {

	private final List<@Nullable ASTInstruction> instructions;

	public ASTCode(List<@Nullable ASTInstruction> instructions) {
		super(ElementType.CODE, instructions);
		this.instructions = instructions;
	}

	public List<@Nullable ASTInstruction> getInstructions() {
		return instructions;
	}

}
