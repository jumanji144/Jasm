package me.darknet.assembler.query;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import org.jetbrains.annotations.NotNull;

/**
 * Information about a variable, including its identity, declaration, and whether it's a parameter.
 *
 * @param identity
 * 		The identity of the variable, which includes its name and type.
 * @param declaration
 * 		The AST node where this variable is declared.
 * @param parameter
 *        {@code true} if this variable is a parameter, {@code false} otherwise.
 * 		Parameters are treated differently from local variables in some contexts, such as method resolution and code generation.
 */
public record VariableInfo(@NotNull VariableIdentity identity,
                           @NotNull ASTIdentifier declaration,
                           boolean parameter) {}
