package me.darknet.assembler.backend.dalvik.instructions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Register operand of a canonical invoke spelled as explicit register words, {@code { words: { v0, v2 } }}.
 * <p>
 * Each entry is one physical register word, in the order written. Nothing about the entries is inferred from the
 * descriptor, so a long or double argument is not widened to its tail and the words are not regrouped into logical
 * arguments. The encoding is chosen from these words alone.
 *
 * @param registers
 * 		Register words in source order.
 */
public record RegisterWords(@NotNull List<RegisterRef> registers) implements RegisterOperands {}
