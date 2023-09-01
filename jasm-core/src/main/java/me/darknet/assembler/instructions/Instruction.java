package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class Instruction<V extends ASTInstructionVisitor> {

    final Operand[] operands;
    final BiConsumer<ASTInstruction, V> translator;

    public Instruction(Operand[] operands, BiConsumer<ASTInstruction, V> translator) {
        this.operands = operands;
        this.translator = translator;
    }

    public void verify(ASTInstruction instruction, ASTProcessor.ParserContext context) {
        if (instruction.getArguments().size() != operands.length) {
            context.throwError(
                    "Expected " + operands.length + " operands, got " + instruction.getArguments().size(),
                    instruction.getLocation()
            );
            return;
        }
        for (int i = 0; i < operands.length; i++) {
            @Nullable
            ASTElement arg = instruction.getArguments().get(i);
            if (arg == null) {
                context.throwError("Expected operand " + i + " to be present", instruction.getLocation());
                return;
            }
            operands[i].verify(context, arg);
        }
    }

    @SuppressWarnings("unchecked")
    public void transform(ASTInstruction instruction, ASTInstructionVisitor visitor) {
        translator.accept(instruction, (V) visitor);
    }

}
