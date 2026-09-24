package me.darknet.assembler.processing;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionLowering;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.OperandValue;
import me.darknet.assembler.instructions.ValidatedOperand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Source-preserving instruction entry with its exact registry definition and typed operands.
 *
 * @param source
 * 		Instruction source node.
 * @param definition
 * 		Definition selected by the target registry.
 * @param operands
 * 		Validated semantic operands, one per declared operand and index-aligned.
 */
public record ProcessedInstruction(@NotNull ASTInstruction source,
                                   @NotNull Instruction<?> definition,
                                   @NotNull List<ValidatedOperand> operands) implements ProcessedCodeEntry {

    public ProcessedInstruction {
        operands = List.copyOf(operands);

        // Operand lookups are by position, so validate that the declared indices match the list order.
        for (int i = 0; i < operands.size(); i++) {
            if (operands.get(i).index() != i) {
                throw new IllegalArgumentException("Operand at position " + i + " declares index " + operands.get(i).index());
            }
        }
    }

    @Override
    public @Nullable OperandValue operand(int index) {
        if (index < 0 || index >= operands.size())
            return null;
        return operands.get(index).value();
    }

    @Override
    public @Nullable InstructionLowering lowering() {
        return definition.lowering();
    }

    @Override
    public @Nullable OperandRole role(int index) {
        return definition.role(index);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OperandValue> @NotNull T operand(int index, @NotNull Class<T> type) {
        OperandValue value = operand(index);
        if (!type.isInstance(value))
            throw new IllegalStateException("Operand " + index + " is not a " + type.getSimpleName());
        return (T) value;
    }
}
