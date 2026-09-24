package me.darknet.assembler.processing;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Semantic view of a source unit that completed target validation without errors.
 *
 * @see SemanticProcessor
 */
public final class ValidatedUnit {
	private final @NotNull List<ASTElement> declarations;
	private final @NotNull TargetContext target;
	private final @NotNull Map<ASTMethod, ProcessedMethod> methods;

	/**
	 * @param partial
	 * 		Recoverable semantic view whose processing produced no errors.
	 */
	ValidatedUnit(@NotNull PartialProcessedUnit partial) {
		Objects.requireNonNull(partial, "partial");
		declarations = partial.declarations();
		target = partial.target();
		methods = partial.methods();
	}

	/**
	 * @return Top-level source declarations.
	 */
	public @NotNull List<ASTElement> declarations() {
		return declarations;
	}

	/**
	 * @return Target context that validated this unit.
	 */
	public @NotNull TargetContext target() {
		return target;
	}

	/**
	 * @return Identity map from source methods to their processed views.
	 */
	public @NotNull Map<ASTMethod, ProcessedMethod> methods() {
		return methods;
	}
}
