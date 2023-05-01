package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTInstruction;

public interface ASTJvmInstructionVisitor {

	void visitInstruction(ASTInstruction instruction);

}
