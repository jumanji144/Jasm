package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

import java.util.ArrayList;
import java.util.List;

public class Modifiers {

	private static final List<String> validModifiers = List.of(
			"public",
			"private",
			"protected",
			"static",
			"final",
			"abstract",
			"strictfp",
			"transient",
			"volatile",
			"synchronized",
			"native",
			"varargs",
			"bridge",
			"synthetic",
			"enum",
			"annotation",
			"module",
			"super",
			"interface",
			"record",
			"sealed",
			"open",
			"non-sealed"
	);
	private final List<ASTIdentifier> modifiers = new ArrayList<>();

	public static boolean isValidModifier(String modifier) {
		return validModifiers.contains(modifier);
	}

	public void addModifier(ASTIdentifier modifier) {
		modifiers.add(modifier);
	}

	public List<ASTIdentifier> getModifiers() {
		return modifiers;
	}

}
