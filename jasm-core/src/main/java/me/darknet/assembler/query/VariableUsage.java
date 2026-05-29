package me.darknet.assembler.query;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a usage of a variable in the code.
 *
 * @param identity
 * 		The identity of the variable being used, if resolved. Otherwise, null.
 * @param name
 * 		The name of the variable being referenced.
 * @param reference
 * 		The AST node where the variable is being referenced.
 * @param instruction
 * 		The instruction in which the variable is being referenced.
 * @param kind
 * 		The kind of variable access (read, write, etc.)
 * @param ambiguous
 * 		Whether the reference is ambiguous (Multiple variables with the same name were found in the scope).
 */
public record VariableUsage(@Nullable VariableIdentity identity,
                            @NotNull String name,
                            @NotNull ASTIdentifier reference,
                            @NotNull ASTInstruction instruction,
                            @NotNull VariableAccessKind kind,
                            boolean ambiguous) {
	public boolean resolved() {
		return identity != null;
	}
}
