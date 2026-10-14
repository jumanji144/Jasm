package me.darknet.assembler.query;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the identity of a variable, which is determined by its name, ordinal, and declaration kind.
 *
 * @param name
 * 		The name of the variable.
 * @param ordinal
 * 		The ordinal position of the variable in its declaration context (method parameters, local variables).
 * @param kind
 * 		The kind of variable declaration (parameter, local variable, etc.)
 */
public record VariableIdentity(@NotNull String name, int ordinal,
                               @NotNull VariableDeclarationKind kind) {}
