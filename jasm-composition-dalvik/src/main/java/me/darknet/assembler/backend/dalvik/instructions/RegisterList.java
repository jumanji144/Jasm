package me.darknet.assembler.backend.dalvik.instructions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Register operand spelled as an explicit list.
 *
 * @param registers
 * 		Registers in source order.
 */
public record RegisterList(@NotNull List<RegisterRef> registers) implements RegisterOperands {
    @Override
    public boolean isRange() {
        return false;
    }
}
