package me.darknet.assembler.backend.jvm.instructions;

import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validated {@code tableswitch} payload.
 *
 * @param min
 * 		Lowest case key. Every case key is implicit from {@code min} upward.
 * @param defaultLabel
 * 		Label targeted when no case key matches.
 * @param caseLabels
 * 		Case labels in source order, one per key from {@code min} to {@code min + size - 1}.
 */
public record TableSwitchPayload(int min, @NotNull String defaultLabel, @NotNull List<String> caseLabels) implements OperandValue {}
