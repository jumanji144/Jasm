package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTInner;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.ast.specific.ASTRecordComponent;
import me.darknet.assembler.visitor.Modifiers;

import java.util.List;

/**
 * Parsers for class attributes.
 */
final class ClassAttributeParsers {
	private ClassAttributeParsers() {}

	/**
	 * @param registry
	 * 		Registry to register the parsers in.
	 */
	static void register(DeclarationRegistry registry) {
		registry.register("signature", ClassAttributeParsers::parseSignature);
		registry.register("sourcefile", ClassAttributeParsers::parseSourceFile);
		registry.register("super", ClassAttributeParsers::parseSuper);
		registry.register("implements", ClassAttributeParsers::parseImplements);
		registry.register("permitted-subclass", ClassAttributeParsers::parsePermittedSubclass);
		registry.register("record-component", ClassAttributeParsers::parseRecordComponent);
		registry.register("outer-class", ClassAttributeParsers::parseOuterClass);
		registry.register("outer-method", ClassAttributeParsers::parseOuterMethod);
		registry.register("nest-host", ClassAttributeParsers::parseNestHost);
		registry.register("nest-member", ClassAttributeParsers::parseNestMember);
		registry.register("inner", ClassAttributeParsers::parseInner);
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed signature attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseSignature(ProcessorContext context, ASTDeclaration declaration) {
		ASTString signature = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.STRING, "signature", declaration
		);
		if (signature != null)
			context.state().setSignature(signature);
		return signature;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed source file attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseSourceFile(ProcessorContext context, ASTDeclaration declaration) {
		ASTString sourceFile = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.STRING, "source file", declaration
		);
		if (sourceFile != null)
			context.state().setSourceFile(sourceFile);
		return sourceFile;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed super class attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseSuper(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier superName = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "super name", declaration
		);
		if (superName != null)
			context.state().setSuperName(superName);
		return superName;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed implemented interface attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseImplements(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier interfaceName = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "interface name", declaration
		);
		if (interfaceName != null)
			context.state().addInterface(interfaceName);
		return interfaceName;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed permitted subclass attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parsePermittedSubclass(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier subclassName = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "interface name", declaration
		);
		if (subclassName != null)
			context.state().addPermittedSubclass(subclassName);
		return subclassName;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed record component attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseRecordComponent(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier name = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "record component name", declaration
		);
		ASTIdentifier desc = context.validateElement(
				context.declarationElement(declaration, 1), ElementType.IDENTIFIER, "record component desc", declaration
		);
		if (name == null || desc == null)
			return null;
		ASTRecordComponent recordComponent = new ASTRecordComponent(name, desc);
		recordComponent.accept(context.state().collectGenericAttributes());
		context.state().addRecordComponent(recordComponent);
		return recordComponent;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed outer class attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseOuterClass(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier className = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "outer class", declaration
		);
		if (className != null)
			context.state().setOuterClass(className);
		return className;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed outer method attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseOuterMethod(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier methodName = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "outer method name", declaration
		);
		ASTIdentifier methodDesc = context.validateElement(
				context.declarationElement(declaration, 1), ElementType.IDENTIFIER, "outer method desc", declaration
		);
		if (methodName == null || methodDesc == null)
			return null;
		ASTOuterMethod outer = new ASTOuterMethod(methodName, methodDesc);
		context.state().setOuterMethod(outer);
		return outer;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed nest host attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseNestHost(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier nestHost = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "nest host", declaration
		);
		if (nestHost != null)
			context.state().setNestHost(nestHost);
		return nestHost;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed nest member attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseNestMember(ProcessorContext context, ASTDeclaration declaration) {
		ASTIdentifier nestMember = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "nest member", declaration
		);
		if (nestMember != null) {
			context.state().addNestMember(nestMember);
		}
		return nestMember;
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed inner class attribute, or {@code null} if the declaration is invalid.
	 */
	private static ASTElement parseInner(ProcessorContext context, ASTDeclaration declaration) {
		List<ASTElement> elements = declaration.elements().stream().map(element -> (ASTElement) element).toList();
		if (elements.isEmpty()) {
			context.throwError("Expected inner class modifiers and body", declaration.location());
			return null;
		}

		int bodyIndex = elements.size() - 1;
		ASTObject body = context.validateElement(
				context.declarationElement(declaration, bodyIndex),
				ElementType.OBJECT,
				"inner class body",
				declaration
		);
		if (body == null)
			return null;

		Modifiers modifiers = ModifierParser.parseModifiers(context, bodyIndex, declaration);
		ASTIdentifier name = context.validateMaybeIdentifier(body.values().get("name"), "inner class name", declaration);
		ASTIdentifier inner = context.validateIdentifier(body.values().get("inner"), "inner class type", declaration);
		ASTIdentifier outer = context.validateMaybeIdentifier(body.values().get("outer"), "outer class type", declaration);
		if (inner == null)
			return null;

		ASTInner innerClass = new ASTInner(modifiers, name, outer, inner);
		context.state().addInner(innerClass);
		return innerClass;
	}
}
