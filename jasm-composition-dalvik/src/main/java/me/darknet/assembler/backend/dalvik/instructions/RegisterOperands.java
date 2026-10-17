package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Register operand of a Dalvik invoke or {@code filled-new-array} instruction.
 * <p>
 * The two shapes keep their source meanings apart: a list names each register it holds, and raw words name the physical
 * register words exactly as written.
 */
public sealed interface RegisterOperands extends OperandValue permits RegisterList, RegisterWords {
    /**
     * @return Registers in the order written.
     */
    @NotNull
    List<RegisterRef> registers();
}
