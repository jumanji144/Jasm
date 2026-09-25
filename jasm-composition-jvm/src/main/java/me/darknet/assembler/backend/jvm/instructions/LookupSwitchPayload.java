package me.darknet.assembler.backend.jvm.instructions;

import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Validated {@code lookupswitch} payload.
 *
 * @param defaultLabel
 * 		Label targeted when no case key matches.
 * @param caseLabels
 * 		Case keys paired with their labels. Keys are already sign- and radix-resolved, so the map is
 * 		keyed by the bit pattern the backend emits.
 */
public record LookupSwitchPayload(@NotNull String defaultLabel, @NotNull Map<Integer, String> caseLabels) implements OperandValue {}
