package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Parses modifiers from a declaration.
 *
 * @see ClassDeclarationParser
 * @see MethodDeclarationParser
 * @see FieldDeclarationParser
 */
final class ModifierParser {
	private ModifierParser() {}

	/**
	 * @param context
	 * 		Processor context to use for error reporting.
	 * @param endIndex
	 * 		Index of the last modifier in the declaration.
	 * 		Modifiers are expected to be the first elements in the declaration,
	 * 		so this is typically the index of the first non-modifier element.
	 * @param declaration
	 * 		Declaration to parse modifiers from.
	 *
	 * @return Parsed modifiers.
	 */
	static Modifiers parseModifiers(ProcessorContext context, int endIndex, ASTDeclaration declaration) {
		Modifiers modifiers = new Modifiers();
		List<@Nullable ASTElement> elements = declaration.elements();
		for (int i = 0; i < endIndex; i++) {
			ASTIdentifier modifier = context.validateElement(
					elements.get(i), ElementType.IDENTIFIER, "access modifier", declaration
			);
			if (modifier == null)
				continue;
			String content = modifier.content();
			if (!Modifiers.isValidModifier(content)) {
				context.throwError("Invalid modifier: " + content, modifier.location());
				continue;
			}
			modifiers.addModifier(modifier);
		}
		return modifiers;
	}
}
