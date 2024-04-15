package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import org.jetbrains.annotations.NotNull;

public interface ASTInstructionVisitor {

    /**
     * Called on every instruction visit.
     *
     * @param instruction
     *                    The instruction
     */
    void visitInstruction(@NotNull ASTInstruction instruction);

    /**
     * Visit a label
     *
     * @param label
     *              the label
     */
    void visitLabel(@NotNull ASTIdentifier label);

    /**
     * Visit a line number
     *
     * @param label
     *              the label
     * @param line
     *              the line number
     */
    void visitLineNumber(ASTIdentifier label, ASTNumber line);

    void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type);

    void visitEnd();

}
