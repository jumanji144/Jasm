package me.darknet.assembler.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Result of a variable query, containing all declarations and usages of the variable(s) in question.
 *
 * @param declarations
 * 		List of variable declarations in a method.
 * @param usages
 * 		List of variable usages in a method.
 */
public record VariableQueryResult(@NotNull List<VariableInfo> declarations,
                                  @NotNull List<VariableUsage> usages) {
	/**
	 * @param name
	 * 		The name of the variable to search for.
	 *
	 * @return The declaration of the variable with the given name, or {@code null} if no such variable exists.
	 * If multiple variables with the same name exist, the first one is returned.
	 */
	public @Nullable VariableInfo declarationOf(@NotNull String name) {
		return declarations.stream()
				.filter(declaration -> declaration.identity().name().equals(name))
				.findFirst().orElse(null);
	}

	/**
	 * @param name
	 * 		The name of the variable to search for.
	 *
	 * @return List of all declarations of the variable with the given name.
	 * Empty list if no such variable exists.
	 */
	public @NotNull List<VariableInfo> declarationsOf(@NotNull String name) {
		return declarations.stream()
				.filter(declaration -> declaration.identity().name().equals(name))
				.toList();
	}

	/**
	 * @param name
	 * 		The name of the variable to search for.
	 *
	 * @return List of all usages of the variable with the given name.
	 * Empty list if no such variable exists.
	 */
	public @NotNull List<VariableUsage> usagesOf(@NotNull String name) {
		return usages.stream()
				.filter(usage -> usage.name().equals(name))
				.toList();
	}

	/**
	 * @param identity
	 * 		The identity of the variable to search for.
	 *
	 * @return List of all usages of the variable with the given identity. Empty list if no such variable exists.
	 */
	public @NotNull List<VariableUsage> readsOf(@NotNull VariableIdentity identity) {
		return usages.stream().filter(usage -> identity.equals(usage.identity()) && usage.kind() == VariableAccessKind.READ).toList();
	}

	/**
	 * @param identity
	 * 		The identity of the variable to search for.
	 *
	 * @return List of all write usages of the variable with the given identity. Empty list if no such variable exists.
	 */
	public @NotNull List<VariableUsage> writesOf(@NotNull VariableIdentity identity) {
		return usages.stream().filter(usage -> identity.equals(usage.identity())
				&& (usage.kind() == VariableAccessKind.WRITE || usage.kind() == VariableAccessKind.INCREMENT)).toList();
	}
}
