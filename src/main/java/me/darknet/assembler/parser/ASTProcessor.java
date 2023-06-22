package me.darknet.assembler.parser;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.ast.specific.*;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Pair;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ASTProcessor {

	public static Result<List<ASTElement>> processAST(List<ASTElement> ast) {
		ParserContext ctx = new ParserContext();
		List<ASTElement> result = new ArrayList<>();
		for (ASTElement astElement : ast) {
			if (astElement instanceof ASTDeclaration) {
				result.add(parseDeclaration(ctx, (ASTDeclaration) astElement));
			} else {
				ctx.throwUnexpectedElementError("declaration", astElement);
			}
		}
		return new Result<>(result, ctx.errorCollector.getErrors());
	}

	private static ASTElement parseDeclaration(ParserContext ctx, ASTDeclaration declaration) {
		String keyword = declaration.getKeyword().getContent().substring(1);
		return ParserRegistry.get(keyword).parse(ctx, declaration);
	}

	private static Modifiers parseModifiers(ParserContext ctx, int endIndex, ASTDeclaration declaration) {
		Modifiers modifiers = new Modifiers();
		List<@Nullable ASTElement> elements = declaration.getElements();
		for (int i = 0; i < endIndex; i++) {
			// modifiers MUST be IDENTIFIER
			ASTIdentifier modifier = ctx.validateElement(elements.get(i), ElementType.IDENTIFIER,
					"class modifier", declaration);
			if(modifier == null) continue;
			String content = modifier.getContent();
			// check if the modifier is valid
			if(!Modifiers.isValidModifier(content)) {
				ctx.throwError("Invalid modifier: " + content, modifier.getLocation());
				continue;
			}
			modifiers.addModifier(modifier);
		}
		return modifiers;
	}

	private static ASTClass parseClass(ParserContext ctx, ASTDeclaration declaration) {
		// first try to find a body, must be at the end
		List<@Nullable ASTElement> elements = declaration.getElements();
		int bodyIndex = elements.size() - 1;
		ASTDeclaration body = ctx.validateElement(elements.get(bodyIndex), ElementType.DECLARATION, "class body",
				declaration);
		if(body == null) return null;
		if(bodyIndex == 0) { // name must be included
			ctx.throwError("Expected class name", body.getLocation());
			return null;
		}
		int nameIndex = bodyIndex - 1;
		// name is a explicit identifier
		ASTIdentifier name = ctx.validateIdentifier(elements.get(nameIndex), "class name", declaration);
		Modifiers modifiers = parseModifiers(ctx, nameIndex, declaration);
		List<ASTElement> classBody = ctx.parseDeclarations(body.getElements(),
				"class member or member attribute", body.getLocation(),
				"field", "method", "annotation", "signature");
		Attributes attributes = ctx.collectAttributes();
		return new ASTClass(modifiers, name, attributes.signature, attributes.annotations,
				attributes.superName, attributes.interfaces, classBody);
	}

	private static ASTField parseField(ParserContext ctx, ASTDeclaration declaration) {
		List<@Nullable ASTElement> elements = declaration.getElements();
		if(elements.size() < 2) {
			ctx.throwError("Expected field name and descriptor", declaration.getLocation());
			return null;
		}
		int lastIndex = elements.size() - 1;
		int descIndex = lastIndex;
		int nameIndex = lastIndex - 1;
		ASTElement last = elements.get(lastIndex);
		ASTValue value = null;
		if(last instanceof ASTObject) {
			descIndex = lastIndex - 1; // if there is a value name and descriptor will be pushed back
			nameIndex = lastIndex - 2;
			ASTObject obj = (ASTObject) last;
			ASTElement elem = obj.getValues().get("value");
			if(!(elem instanceof ASTValue) || obj.getValues().size() != 1) {
				ctx.throwUnexpectedElementError("field value", elem == null ? last : elem);
				return null;
			}
			value = (ASTValue) elem;
		} else if (!(last instanceof ASTIdentifier)) {
			ctx.throwUnexpectedElementError("field descriptor or field value", last == null ? declaration : last);
			return null;
		}
		ASTIdentifier desc = ctx.validateIdentifier(elements.get(descIndex), "field descriptor", declaration);
		ASTIdentifier name = ctx.validateIdentifier(elements.get(nameIndex), "field name", declaration);
		Modifiers modifiers = parseModifiers(ctx, nameIndex, declaration);
		Attributes attributes = ctx.collectAttributes();
		return new ASTField(modifiers, name, desc, attributes.annotations, attributes.signature, value);
	}

	private static ASTMethod parseMethod(ParserContext ctx, ASTDeclaration declaration) {
		List<@Nullable ASTElement> elements = declaration.getElements();
		if(elements.size() < 3) {
			ctx.throwError("Expected method name, descriptor and body", declaration.getLocation());
			return null;
		}
		int lastIndex = elements.size() - 1;
		ASTObject body = ctx.validateEmptyableElement(elements.get(lastIndex), ElementType.OBJECT, "method body", declaration);
	    if(body == null) return null;
		List<ASTIdentifier> parameters = Collections.emptyList();
		if(body.getValues().containsKey("parameters")) {
			ASTArray array = ctx.validateEmptyableElement(body.getValues().get("parameters"), ElementType.ARRAY,
					"method parameters", declaration);
			if(array != null)
				parameters = ctx.validateArray(array, ElementType.IDENTIFIER, "method parameter", declaration);
		}
	    ASTCode code = null;
		if(body.getValues().containsKey("code")) {
			code = ctx.validateEmptyableElement(body.getValues().get("code"), ElementType.CODE,
					"method code", declaration);
		}
		int nameIndex = lastIndex - 2;
		int descIndex = lastIndex - 1;
		ASTIdentifier name = ctx.validateIdentifier(elements.get(nameIndex), "method name", declaration);
		ASTIdentifier desc = ctx.validateIdentifier(elements.get(descIndex), "method descriptor", declaration);
		Modifiers modifiers = parseModifiers(ctx, nameIndex, declaration);
		Attributes attributes = ctx.collectAttributes();
		return new ASTMethod(modifiers, name, desc, attributes.signature, attributes.annotations, parameters, code);
	}

	static {
		ParserRegistry.register("class", ASTProcessor::parseClass);
		ParserRegistry.register("field", ASTProcessor::parseField);
		ParserRegistry.register("method", ASTProcessor::parseMethod);
		ParserRegistry.register("signature", (ctx, decl) -> {
			ctx.addSignature(ctx.validateElement(decl.getElements().get(0), ElementType.IDENTIFIER,
					"signature", decl));
			return null;
		});
		ParserRegistry.register("super", (ctx, decl) -> {
			ctx.addSuperName(ctx.validateElement(decl.getElements().get(0), ElementType.IDENTIFIER,
					"super name", decl));
			return null;
		});
		ParserRegistry.register("interface", (ctx, decl) -> {
			ctx.addInterface(ctx.validateElement(decl.getElements().get(0), ElementType.IDENTIFIER,
					"interface name", decl));
			return null;
		});
	}

	private static class Attributes {

		private ASTIdentifier signature;
		private ASTIdentifier superName;
		private final List<ASTIdentifier> interfaces = new ArrayList<>();
		private final List<ASTAnnotation> annotations = new ArrayList<>();

	}

	private static class ParserContext {

		private Attributes attributes = new Attributes();
		private final ErrorCollector errorCollector = new ErrorCollector();

		public void throwError(String message, Location location) {
			errorCollector.addError(new Error(message, location));
		}

		public boolean isNull(Object value, String expected, Location location) {
			if (value == null) {
				throwError("Expected " + expected + " but got nothing", location);
				return true;
			}
			return false;
		}

		public boolean isNotType(ASTElement element, ElementType type, String expected) {
			if (element.getType() != type) {
				throwUnexpectedElementError(expected, element);
				return true;
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		<T> T validateElement(ASTElement e, ElementType expectedElementType, String description, ASTElement parent) {
			if(isNull(e, description, parent.getLocation())) return null;
			if(isNotType(e, expectedElementType, description)) return null;
			return (T) e;
		}

		@SuppressWarnings("unchecked")
		<T> T validateEmptyableElement(ASTElement e, ElementType expectedElementType, String description, ASTElement parent) {
			if(isNull(e, description, parent.getLocation())) return null;
			if(e.getType() == ElementType.EMPTY) {
				switch (expectedElementType) {
					case OBJECT:
						return (T) ASTEmpty.EMPTY_OBJECT;
					case ARRAY:
						return (T) ASTEmpty.EMPTY_ARRAY;
					case CODE:
						return (T) ASTEmpty.EMPTY_CODE;
					default:
						throwUnexpectedElementError(description, e);
						return null;
				}
			}
			if(isNotType(e, expectedElementType, description)) return null;
			return (T) e;
		}

		<T> List<T> validateArray(ASTArray array, ElementType expectedElements, String description, ASTElement parent) {
			if(isNull(array, description, parent.getLocation())) return Collections.emptyList();
			List<T> result = new ArrayList<>();
			for (ASTElement element : array.getValues()) {
				if(isNull(element, description, parent.getLocation())) continue;
				assert element != null;
				if(isNotType(element, expectedElements, description)) continue;
				result.add((T) element);
			}
			return result;
		}

		ASTIdentifier validateIdentifier(ASTElement e, String description, ASTElement parent) {
			if(isNull(e, description, parent.getLocation())) return null;
			// can be NUMBER or IDENTIFIER
			if(!(e instanceof ASTLiteral)) {
				throwUnexpectedElementError(description, e);
				return null;
			}
			if(e.getType() == ElementType.NUMBER) {
				// convert to identifier
				return new ASTIdentifier(e.getValue()); // rewrap
			}
			return (ASTIdentifier) e;
		}

		List<ASTElement> parseDeclarations(List<@Nullable ASTElement> elements, String expected, Location parent,
										   String... types) {
			List<ASTElement> result = new ArrayList<>();
			Location lastLocation = parent;
			for (ASTElement element : elements) {
				if(isNull(element, expected, lastLocation)) continue;
				assert element != null;
				lastLocation = element.getLocation();
				if(isNotType(element, ElementType.DECLARATION, expected)) continue;
				ASTDeclaration declaration = (ASTDeclaration) element;
				String keyword = declaration.getKeyword().getContent().substring(1);
				boolean found = false;
				for (String type : types) {
					if(keyword.equals(type)) {
						found = true;
						break;
					}
				}
				if(!found) {
					throwUnexpectedElementError(expected, element);
					continue;
				}
				result.add(parseDeclaration(this, declaration));
			}
			return result;
		}

		public void throwUnexpectedElementError(String expected, ASTElement actual) {
			throwError("Expected " + expected + " but got " + actual.getType().toString(),
					actual.getLocation());
		}

		public Attributes collectAttributes() {
			Attributes attributes = this.attributes;
			this.attributes = new Attributes();
			return attributes;
		}

		public void addSignature(ASTIdentifier signature) {
			if(attributes.signature != null) {
				throwError("Signature already defined", signature.getLocation());
			} else {
				attributes.signature = signature;
			}
		}

		public void addAnnotation(ASTAnnotation annotation) {
			attributes.annotations.add(annotation);
		}

		public void addSuperName(ASTIdentifier superName) {
			if(attributes.superName != null) {
				throwError("Super name already defined", superName.getLocation());
			} else {
				attributes.superName = superName;
			}
		}

		public void addInterface(ASTIdentifier interfaceName) {
			attributes.interfaces.add(interfaceName);
		}

	}

	@FunctionalInterface
	private interface DeclarationParser<T extends ASTElement> {
		T parse(ParserContext ctx, ASTDeclaration declaration);
	}

	private static class ParserRegistry {

		private static final DeclarationParser<? extends ASTElement> DEFAULT_PARSER = (ctx, declaration) -> {
			ctx.throwError("Unknown declaration: " + declaration.getKeyword().getContent(),
					declaration.getKeyword().getValue().getLocation());
			return null;
		};

		private final static Map<String, DeclarationParser<?>> parsers = new HashMap<>();

		public static void register(String keyword, DeclarationParser<?> parser) {
			parsers.put(keyword, parser);
		}

		public static DeclarationParser<?> get(String keyword) {
			return parsers.getOrDefault(keyword, DEFAULT_PARSER);
		}

	}
}
