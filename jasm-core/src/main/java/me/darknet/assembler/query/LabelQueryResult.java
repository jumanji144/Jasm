package me.darknet.assembler.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Information about label declarations and usages in a method.
 *
 * @param declarations
 * 		List of label declarations in a method.
 * @param usages
 * 		List of label usages in a method.
 */
public record LabelQueryResult(@NotNull List<LabelInfo> declarations,
                               @NotNull List<LabelUsage> usages) {
	/**
	 * @param name
	 * 		The name of the label to search for.
	 *
	 * @return The first declaration of the label with the given name, or {@code null} if none exists.
	 */
	public @Nullable LabelInfo declarationOf(@NotNull String name) {
		return declarations.stream()
				.filter(declaration -> declaration.name().equals(name))
				.findFirst()
				.orElse(null);
	}

	/**
	 * @param name
	 * 		The name of the label to search for.
	 *
	 * @return All declarations of the label with the given name. Empty list if no such label exists.
	 */
	public @NotNull List<LabelInfo> declarationsOf(@NotNull String name) {
		return declarations.stream()
				.filter(declaration -> declaration.name().equals(name))
				.toList();
	}

	/**
	 * @param usage
	 * 		The label usage to find the declaration of.
	 *
	 * @return The declaration of the given label usage, or {@code null} if it cannot be resolved.
	 */
	public @Nullable LabelInfo declarationOf(@NotNull LabelUsage usage) {
		return usage.label();
	}

	/**
	 * @param name
	 * 		The name of the label to search for.
	 *
	 * @return All usages of a label with the given name. Empty list if no such label exists.
	 */
	public @NotNull List<LabelUsage> usagesOf(@NotNull String name) {
		return usages.stream().filter(usage -> usage.name().equals(name)).toList();
	}

	/**
	 * @param label
	 * 		The label to find references to.
	 *
	 * @return List of all usages that reference the given label. Empty list if no such usages exist.
	 */
	public @NotNull List<LabelUsage> referencesTo(@NotNull LabelInfo label) {
		return usages.stream().filter(usage -> label.equals(usage.label())).toList();
	}
}
