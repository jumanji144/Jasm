package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Register operand of a Dalvik invoke or {@code filled-new-array} instruction.
 */
public sealed interface RegisterOperands extends OperandValue permits RegisterList, RegisterRange {
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
