package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTBool;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.util.DescriptorUtil;
import me.darknet.assembler.util.ElementMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for annotation declarations.
 */
final class AnnotationDeclarationParser {
	private AnnotationDeclarationParser() {}

	static void register(DeclarationRegistry registry) {
		registry.register("annotation", (context, declaration) -> parseAnnotation(context, true, false, true, declaration));
		registry.register("visible-annotation",
				(context, declaration) -> parseAnnotation(context, true, false, true, declaration));
		registry.register("invisible-annotation",
				(context, declaration) -> parseAnnotation(context, false, false, true, declaration));
		registry.register("type-annotation",
				(context, declaration) -> parseAnnotation(context, true, true, true, declaration));
		registry.register("type-visible-annotation",
				(context, declaration) -> parseAnnotation(context, true, true, true, declaration));
		registry.register("type-invisible-annotation",
				(context, declaration) -> parseAnnotation(context, false, true, true, declaration));
		registry.register("enum", AnnotationDeclarationParser::parseEnum);
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed annotation, or {@code null} if the declaration is invalid.
	 */
	static ASTAnnotation parseEmbeddedAnnotation(ProcessorContext context, ASTDeclaration declaration) {
		return switch (DeclarationRegistry.keyword(declaration)) {
			case "annotation", "visible-annotation" -> parseAnnotation(context, true, false, false, declaration);
			case "invisible-annotation" -> parseAnnotation(context, false, false, false, declaration);
			case "type-annotation", "type-visible-annotation" ->
					parseAnnotation(context, true, true, false, declaration);
			case "type-invisible-annotation" -> parseAnnotation(context, false, true, false, declaration);
			default -> {
				context.throwUnexpectedElementError("annotation declaration", declaration);
				yield null;
			}
		};
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param value
	 * 		Element to validate as an annotation value.
	 *
	 * @return Validated annotation value, or {@code null} if the element is not a valid annotation value.
	 */
	static ASTElement validateElementValue(ProcessorContext context, ASTElement value) {
		switch (value.type()) {
			case NUMBER, STRING, CHARACTER -> {
				return value;
			}
			case IDENTIFIER -> {
				ASTIdentifier identifier = (ASTIdentifier) value;
				return switch (identifier.content().toLowerCase()) {
					case "true", "false" -> new ASTBool(identifier.value());
					case "nan", "nand", "nanf",
					     "+infinity", "+infinityd", "infinity", "infinityd",
					     "+infinityf", "infinityf", "-infinity", "-infinityd", "-infinityf" ->
							new ASTNumber(identifier.value());
					default -> {
						if (!DescriptorUtil.isValidFieldDescriptor('L' + identifier.literal() + ';')) {
							context.throwUnexpectedElementError(
									"Expected class type, boolean, or special number",
									value
							);
							yield null;
						}
						yield value;
					}
				};
			}
			case EMPTY -> {
				return ASTEmpty.EMPTY_ARRAY;
			}
			case DECLARATION -> {
				ASTDeclaration declaration = (ASTDeclaration) value;
				if (declaration.keyword() == null) {
					if (declaration.elements().size() != 1) {
						context.throwUnexpectedElementError("annotation value", value);
						return null;
					}
					ASTElement nested = validateElementValue(context, declaration.elements().getFirst());
					if (nested == null)
						return null;
					return new ASTArray(Collections.singletonList(nested));
				}

				return switch (DeclarationRegistry.keyword(declaration)) {
					case "enum" -> parseEnum(context, declaration);
					case "annotation", "visible-annotation", "invisible-annotation", "type-annotation",
					     "type-visible-annotation", "type-invisible-annotation" ->
							parseEmbeddedAnnotation(context, declaration);
					default -> {
						context.throwUnexpectedElementError("annotation value", value);
						yield null;
					}
				};
			}
			case ARRAY -> {
				ASTArray array = (ASTArray) value;
				List<ASTElement> elements = new ArrayList<>();
				for (ASTElement arrayValue : array.values()) {
					if (arrayValue == null) {
						continue;
					}
					ASTElement nested = validateElementValue(context, arrayValue);
					if (nested != null) {
						elements.add(nested);
					}
				}
				return new ASTArray(elements);
			}
			default -> {
				context.throwUnexpectedElementError("annotation value", value);
				return null;
			}
		}
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed enum, or {@code null} if the declaration is invalid.
	 */
	private static ASTEnum parseEnum(ProcessorContext context, ASTDeclaration declaration) {
		if (!context.isInState(ProcessorFlag.IN_ANNOTATION)) {
			context.throwError("enum declaration outside of annotation", declaration.location());
			return null;
		}
		ASTIdentifier type = context.validateElement(
				context.declarationElement(declaration, 0), ElementType.IDENTIFIER, "enum type", declaration
		);
		ASTIdentifier name = context.validateElement(
				context.declarationElement(declaration, 1), ElementType.IDENTIFIER, "enum name", declaration
		);

		ASTIdentifier fieldType = null;
		if (declaration.elements().size() >= 3) {
			fieldType = context.validateElement(
					context.declarationElement(declaration, 2),
					ElementType.IDENTIFIER,
					"enum field type",
					declaration
			);
		}
		if (type == null || name == null)
			return null;
		return new ASTEnum(type, name, fieldType);
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param visible
	 * 		Whether the annotation is visible at runtime.
	 * @param typeAnnotation
	 * 		Whether the annotation is a type annotation.
	 * @param addToState
	 * 		Whether to add the parsed annotation to the processor state.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed annotation, or {@code null} if the declaration is invalid.
	 */
	private static ASTAnnotation parseAnnotation(ProcessorContext context, boolean visible, boolean typeAnnotation,
	                                             boolean addToState, ASTDeclaration declaration) {
		if (declaration.elements().size() != 2) {
			context.throwError("Expected annotation type and values", declaration.location());
			return null;
		}

		ASTIdentifier type = context.validateIdentifier(
				context.declarationElement(declaration, 0), "annotation type", declaration
		);
		if (type == null)
			return null;

		ASTNumber typeRef = null;
		ASTIdentifier typePath = null;
		ASTObject values;
		if (typeAnnotation) {
			ASTObject typeData = context.validateObject(
					context.declarationElement(declaration, 1),
					"type annotation data",
					declaration,
					"location",
					"values"
			);
			if (typeData == null)
				return null;

			ASTObject location = context.validateObject(
					typeData.value("location"),
					"type annotation location",
					typeData,
					"ref",
					"path"
			);
			if (location == null)
				return null;

			typeRef = context.validateElement(location.value("ref"), ElementType.NUMBER, "type ref", location);
			typePath = context.validateIdentifier(location.value("path"), "type path", location);
			values = context.validateEmptyableElement(
					typeData.value("values"), ElementType.OBJECT, "type annotation values", typeData
			);
		} else {
			values = context.validateEmptyableElement(
					context.declarationElement(declaration, 1), ElementType.OBJECT, "annotation values", declaration
			);
		}
		if (values == null)
			return null;

		context.enterState(ProcessorFlag.IN_ANNOTATION);
		ElementMap<ASTIdentifier, ASTElement> map = new ElementMap<>();
		for (var pair : values.values().pairs()) {
			ASTIdentifier key = context.validateIdentifier(pair.first(), "annotation value key", declaration);
			ASTElement nestedValue = validateElementValue(context, pair.second());
			if (key != null && nestedValue != null)
				map.put(key, nestedValue);
		}
		context.leaveState(ProcessorFlag.IN_ANNOTATION);

		ASTAnnotation annotation = new ASTAnnotation(visible, type, map, typeRef, typePath);
		if (addToState && !context.isInState(ProcessorFlag.SKIP_PENDING_ANNOTATION)) {
			if (annotation.isTypeAnnotation()) {
				if (visible) {
					context.state().addVisibleTypeAnnotation(annotation);
				} else {
					context.state().addInvisibleTypeAnnotation(annotation);
				}
			} else if (visible) {
				context.state().addVisibleAnnotation(annotation);
			} else {
				context.state().addInvisibleAnnotation(annotation);
			}
		}
		return annotation;
	}
}
