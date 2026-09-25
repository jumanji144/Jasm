package me.darknet.assembler.backend.dalvik.instructions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Register operand spelled as an explicit first/last range.
 *
 * @param first
 * 		Lowest register of the range.
 * @param last
 * 		Highest register of the range.
 */
public record RegisterRange(@NotNull RegisterRef first, @NotNull RegisterRef last) implements RegisterOperands {
    @Override
    public @NotNull List<RegisterRef> registers() {
        return List.of(first, last);
    }

    @Override
    public boolean isRange() {
        return true;
    }
}
