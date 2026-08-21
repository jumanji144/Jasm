package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.visitor.Modifiers;

import java.util.List;

/**
 * Parser for field declarations.
 */
final class FieldDeclarationParser {
	private FieldDeclarationParser() {}

	/**
	 * @param registry
	 * 		Registry to register the parser in.
	 */
	static void register(DeclarationRegistry registry) {
		registry.register("field", FieldDeclarationParser::parseField);
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed field, or {@code null} if the declaration is invalid.
	 */
	private static ASTField parseField(ProcessorContext context, ASTDeclaration declaration) {
		List<ASTElement> elements = declaration.elements().stream().map(element -> element).toList();
		if (elements.size() < 2) {
			context.throwError("Expected field name and descriptor", declaration.location());
			return null;
		}

		int lastIndex = elements.size() - 1;
		int descIndex = lastIndex;
		int nameIndex = lastIndex - 1;
		ASTElement last = elements.get(lastIndex);
		ASTValue value = null;
		if (last instanceof ASTObject object) {
			descIndex = lastIndex - 1;
			nameIndex = lastIndex - 2;
			ASTElement fieldValue = object.values().get("value");
			if (!(fieldValue instanceof ASTValue) || object.values().size() != 1) {
				context.throwUnexpectedElementError("field value", fieldValue == null ? last : fieldValue);
				return null;
			}
			value = (ASTValue) fieldValue;
		} else if (!(last instanceof ASTIdentifier)) {
			context.throwUnexpectedElementError("field descriptor or field value", last == null ? declaration : last);
			return null;
		}

		ASTIdentifier desc = context.validateIdentifier(
				context.declarationElement(declaration, descIndex), "field descriptor", declaration
		);
		ASTIdentifier name = context.validateIdentifier(
				context.declarationElement(declaration, nameIndex), "field name", declaration
		);
		if (desc == null || name == null) {
			return null;
		}

		Modifiers modifiers = ModifierParser.parseModifiers(context, nameIndex, declaration);
		return new ASTField(modifiers, name, desc, value).accept(context.state().collectAttributes());
	}
}
