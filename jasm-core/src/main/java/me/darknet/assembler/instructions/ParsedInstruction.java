package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.visitor.ASTInstructionVisitor;

public record ParsedInstruction(ASTInstruction ast, Instruction<?> ir) {

    public void accept(ASTInstructionVisitor visitor) {
        if(ast instanceof ASTLabel label) {
            visitor.visitLabel(label.identifier());
        } else {
            visitor.visitInstruction(ast);
            ir.transform(ast, visitor);
        }
    }

}
