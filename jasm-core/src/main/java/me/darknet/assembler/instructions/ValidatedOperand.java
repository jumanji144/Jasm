package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Verified source operand with its position, resolved value, and original AST element.
 * <p>
 * Operand roles remain on {@link InstructionMetadata}; use {@link SemanticInstruction#role(int)} to query
 * the role for this operand.
 *
 * @param index
 * 		Zero-based operand position, always equal to the entry's position in the operand list.
 * @param value
 * 		Typed value produced by the operand's resolver, or {@code null} when the schema declares no resolver.
 * @param source
 * 		Original source element preserving range and ownership.
 */
public record ValidatedOperand(int index, @Nullable OperandValue value, @NotNull ASTElement source) {
    public ValidatedOperand {
        if (index < 0) {
            throw new IllegalArgumentException("Operand index must not be negative: " + index);
        }
    }
}
