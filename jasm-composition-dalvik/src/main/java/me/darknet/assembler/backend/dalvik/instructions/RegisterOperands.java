package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Register operand of a Dalvik invoke or {@code filled-new-array} instruction.
 * <p>
 * The three shapes keep their source meanings apart: a list names registers, a range names its endpoints, and raw words
 * name the physical register words exactly as written.
 */
public sealed interface RegisterOperands extends OperandValue permits RegisterList, RegisterRange, RegisterWords {
    /**
     * @return Registers in allocation order.
     */
    @NotNull
    List<RegisterRef> registers();

    /**
     * @return {@code true} when the source spelled an explicit first/last range.
     */
    boolean isRange();
}
