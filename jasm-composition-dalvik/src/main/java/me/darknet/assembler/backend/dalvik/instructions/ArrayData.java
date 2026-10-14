package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validated {@code fill-array-data} payload.
 *
 * @param elementWidth
 * 		Element width in bytes, either declared by the payload or inferred from the largest literal.
 * @param values
 * 		Numeric literals to encode, in source order.
 */
public record ArrayData(int elementWidth, @NotNull List<ASTNumber> values) implements OperandValue {}
