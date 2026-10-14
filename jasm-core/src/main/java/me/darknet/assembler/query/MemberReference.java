package me.darknet.assembler.query;

import org.jetbrains.annotations.NotNull;

/**
 * A field or method reference resolved from an instruction.
 *
 * @param owner
 * 		Internal name of the type declaring the member.
 * @param name
 * 		Member name.
 * @param descriptor
 * 		Member descriptor, as written in the instruction.
 * @param isMethod
 *        {@code true} for a method reference, {@code false} for a field reference.
 */
public record MemberReference(@NotNull String owner, @NotNull String name, @NotNull String descriptor,
                              boolean isMethod) {}
