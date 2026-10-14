package me.darknet.assembler.processing;

import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.instructions.OperandValue;
import me.darknet.assembler.instructions.ValidatedOperand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Source-preserving label entry in a processed method body.
 *
 * @param source
 * 		Label source node.
 */
public record ProcessedLabel(@NotNull ASTLabel source) implements ProcessedCodeEntry {
    @Override
    public @NotNull List<ValidatedOperand> operands() {
        return List.of();
    }

    @Override
    public @Nullable OperandValue operand(int index) {
        return null;
    }

    @Override
    public <T extends OperandValue> @NotNull T operand(int index, @NotNull Class<T> type) {
        throw new IllegalStateException("Label entries carry no operands");
    }
}
