package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validated {@code packed-switch} payload.
 *
 * @param first
 * 		Key of the first target; every later target is keyed by an implicit increment.
 * @param targets
 * 		Target labels in source order.
 */
public record PackedSwitchPayload(int first, @NotNull List<String> targets) implements OperandValue {}
