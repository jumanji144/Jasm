package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Validated {@code sparse-switch} payload.
 *
 * @param targets
 * 		Case keys paired with their target labels. Keys are already sign- and radix-resolved.
 */
public record SparseSwitchPayload(@NotNull Map<Integer, String> targets) implements OperandValue {}
