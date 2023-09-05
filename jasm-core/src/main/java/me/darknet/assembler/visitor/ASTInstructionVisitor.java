package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;

public interface ASTInstructionVisitor {

    /**
     * Called on every instruction visit
     *
     * @param instruction
     *                    The instruction
     */
    void visitInstruction(ASTInstruction instruction);

    /**
     * Visit a label
     *
     * @param label
     *              the label
     */
    void visitLabel(ASTIdentifier label);

    void visitEnd();

}
