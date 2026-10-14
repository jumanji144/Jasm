package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a list of modifiers for a class, method, or field.
 */
public class Modifiers {
	private static final List<String> validModifiers = List.of(
			"public", "private", "protected", "static", "final", "abstract", "strictfp", "transient", "volatile",
			"synchronized", "native", "varargs", "bridge", "synthetic", "enum", "annotation", "module", "super",
			"interface", "record", "sealed", "open", "non-sealed", "constructor", "declared-synchronized"
	);
	private final List<ASTIdentifier> modifiers = new ArrayList<>();

	/**
	 * @param modifier
	 * 		Modifier to check for.
	 *
	 * @return {@code true} if the modifier is valid, {@code false} otherwise.
	 */
	public static boolean isValidModifier(String modifier) {
		return validModifiers.contains(modifier);
	}

	/**
	 * @return List of modifier keywords accepted by the parser.
	 */
	public static List<String> getValidModifiers() {
		return validModifiers;
	}

	/**
	 * Adds a modifier to the list of modifiers.
	 *
	 * @param modifier
	 * 		Modifier to add.
	 */
	public void addModifier(ASTIdentifier modifier) {
		modifiers.add(modifier);
	}

	/**
	 * @param modifier
	 * 		Modifier to check for.
	 *
	 * @return {@code true} if the modifier is present, {@code false} otherwise.
	 */
	public boolean hasModifier(String modifier) {
		return modifiers.stream().anyMatch(i -> modifier.equals(i.content()));
	}

	/**
	 * @return List of modifiers.
	 */
	public List<ASTIdentifier> getModifiers() {
		return CollectionUtil.immutableCopy(modifiers);
	}

	@Override
	public String toString() {
		return modifiers.stream().map(ASTIdentifier::literal).collect(Collectors.joining(", "));
	}
}
