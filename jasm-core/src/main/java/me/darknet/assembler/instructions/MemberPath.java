package me.darknet.assembler.instructions;

import org.jetbrains.annotations.NotNull;

/**
 * Owner/name split of a source member reference such as {@code java/lang/System.out}.
 *
 * @param owner
 * 		Declaring type or class of the member.
 * @param name
 * 		Member name within {@code owner}.
 */
public record MemberPath(@NotNull String owner, @NotNull String name) implements OperandValue {}
