package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTInstruction;

public interface ASTInstructionVisitor {

    /**
     * Called on every instruction visit
     *
     * @param instruction
     *                    The instruction
     */
    void visitInstruction(ASTInstruction instruction);

}
