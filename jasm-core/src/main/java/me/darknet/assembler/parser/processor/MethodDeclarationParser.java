package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTException;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.descriptor.DescriptorForm;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.MethodAttributeParser;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for method declarations.
 */
final class MethodDeclarationParser {
	private MethodDeclarationParser() {}

	/**
	 * @param registry
	 * 		Registry to register the parser in.
	 */
	static void register(DeclarationRegistry registry) {
		registry.register("method", MethodDeclarationParser::parseMethod);
	}

	/**
	 * @param context
	 * 		Context to parse the declaration in.
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed method, or {@code null} if the declaration is invalid.
	 */
	private static ASTMethod parseMethod(ProcessorContext context, ASTDeclaration declaration) {
		List<ASTElement> elements = declaration.elements().stream().map(element -> element).toList();
		if (elements.size() < 3) {
			context.throwError("Expected method name, descriptor and body", declaration.location());
			return null;
		}

		int lastIndex = elements.size() - 1;
		ASTObject body = context.validateEmptyableElement(
				context.declarationElement(declaration, lastIndex), ElementType.OBJECT, "method body", declaration
		);
		if (body == null)
			return null;

		List<ASTIdentifier> parameters = Collections.emptyList();
		ASTElement parametersElement = body.values().get(ProcessorKeywords.PARAMETERS);
		if (parametersElement != null) {
			ASTArray parameterArray = context.validateEmptyableElement(
					parametersElement, ElementType.ARRAY, "method parameters", declaration
			);
			if (parameterArray != null) {
				parameters = context.validateArray(parameterArray, ElementType.IDENTIFIER, "method parameter", declaration);
			}
		}

		Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = parseParameterAnnotations(context, declaration, body);
		List<ASTIdentifier> declaredExceptions = parseDeclaredExceptions(context, declaration, body);

		ASTElement defaultValueElement = body.values().get(ProcessorKeywords.DEFAULT_VALUE);
		if (defaultValueElement != null && context.supports(AnnotationCapability.ANNOTATION_DEFAULT_VALUES, defaultValueElement)) {
			context.enterState(ProcessorFlag.IN_ANNOTATION);
			AnnotationDeclarationParser.validateElementValue(context, defaultValueElement);
			context.leaveState(ProcessorFlag.IN_ANNOTATION);
		}

		List<ASTException> exceptions = parseExceptions(context, declaration, body);

		ASTCode code = null;
		ASTElement codeElement = body.values().get(ProcessorKeywords.CODE);
		if (codeElement != null)
			code = context.validateEmptyableElement(codeElement, ElementType.CODE, "method code", declaration);

		ElementMap<ASTIdentifier, @Nullable ASTElement> methodAttributes = parseMethodAttributes(context, declaration, body);

		int nameIndex = lastIndex - 2;
		int descIndex = lastIndex - 1;
		ASTIdentifier name = context.validateIdentifier(context.declarationElement(declaration, nameIndex), "method name", declaration);
		ASTIdentifier desc = context.validateDescriptor(context.declarationElement(declaration, descIndex), DescriptorForm.METHOD, "method descriptor", declaration);
		if (name == null || desc == null)
			return null;

		Modifiers modifiers = ModifierParser.parseModifiers(context, nameIndex, declaration);
		ASTMethod method = new ASTMethod(
				modifiers,
				name,
				desc,
				parameters,
				parameterAnnotations,
				declaredExceptions,
				defaultValueElement,
				exceptions,
				code,
				methodAttributes
		);
		return method.accept(context.state().collectGenericAttributes());
	}

	private static ElementMap<ASTIdentifier, @Nullable ASTElement> parseMethodAttributes(ProcessorContext context, ASTDeclaration declaration, ASTObject body) {
		ElementMap<ASTIdentifier, @Nullable ASTElement> attributes = new ElementMap<>();
		for (var pair : body.values().pairs()) {
			ASTIdentifier key = pair.first();
			if (ProcessorKeywords.getCommonMethodBodyKeywords().contains(key.content()))
				continue;
			MethodAttributeParser parser = context.getTarget().methodAttributes().get(key.content());
			if (parser == null) {
				context.throwError(DiagnosticCode.UNSUPPORTED_FORM, "Method attribute '" + key.content() + "' is not supported by this target", key.location());
				continue;
			}
			parser.parse(context, pair.second(), declaration);
			attributes.put(key, pair.second());
		}
		return attributes;
	}

	/**
	 * @param context
	 * 		Context to parse the parameter annotations in.
	 * @param declaration
	 * 		Declaration to parse.
	 * @param body
	 * 		Method body to parse the parameter annotations from.
	 *
	 * @return Map of parameter names to their annotations.
	 * Parameters without annotations are not included in the map.
	 */
	private static Map<ASTIdentifier, List<ASTAnnotation>> parseParameterAnnotations(ProcessorContext context,
	                                                                                 ASTDeclaration declaration, ASTObject body) {
		ASTElement parameterAnnotationsElement = body.values().get(ProcessorKeywords.PARAMETER_ANNOTATIONS);
		if (parameterAnnotationsElement == null) {
			return new IdentityHashMap<>();
		}
		if (!context.supports(AnnotationCapability.PARAMETER_ANNOTATIONS, parameterAnnotationsElement))
			return new IdentityHashMap<>();

		ASTObject parameterAnnotationsObject;
		if (parameterAnnotationsElement.type() == ElementType.EMPTY) {
			parameterAnnotationsObject = ASTEmpty.EMPTY_OBJECT;
		} else if (parameterAnnotationsElement.type() == ElementType.OBJECT) {
			parameterAnnotationsObject = (ASTObject) parameterAnnotationsElement;
		} else {
			context.throwError(DiagnosticCode.PAYLOAD_SHAPE, "Expected parameter annotations object", parameterAnnotationsElement.location());
			return new IdentityHashMap<>();
		}

		Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = new IdentityHashMap<>();
		context.enterState(ProcessorFlag.SKIP_PENDING_ANNOTATION);
		for (var pair : parameterAnnotationsObject.values().pairs()) {
			ASTIdentifier parameter = context.validateMaybeIdentifier(pair.first(), "parameter annotation target", declaration);
			if (parameter == null)
				continue;

			List<ASTAnnotation> collected = collectParameterAnnotations(context, declaration, pair.second());
			if (!collected.isEmpty())
				parameterAnnotations.put(parameter, collected);
		}
		context.leaveState(ProcessorFlag.SKIP_PENDING_ANNOTATION);
		return parameterAnnotations;
	}

	/**
	 * @param context
	 * 		Context to parse the parameter annotations in.
	 * @param declaration
	 * 		Declaration to parse.
	 * @param annotationList
	 * 		List of annotations to parse.
	 * 		This can be either a single annotation or an array of annotations.
	 *
	 * @return List of parsed annotations. If the annotation list is invalid, an empty list is returned and errors are reported to the context.
	 */
	private static List<ASTAnnotation> collectParameterAnnotations(ProcessorContext context, ASTDeclaration declaration,
	                                                               ASTElement annotationList) {
		List<ASTAnnotation> collected = new ArrayList<>();
		if (annotationList == null) {
			context.throwError(DiagnosticCode.PAYLOAD_SHAPE, "Expected parameter annotation list", declaration.location());
			return collected;
		}
		if (annotationList instanceof ASTDeclaration singleAnnotation) {
			if (singleAnnotation.keyword() == null) {
				for (ASTElement element : singleAnnotation.elements()) {
					if (!(element instanceof ASTDeclaration nestedDeclaration)) {
						context.throwError(DiagnosticCode.PAYLOAD_SHAPE,
								"Expected parameter annotation declaration",
								element == null ? singleAnnotation.location() : element.location());
						continue;
					}
					ASTAnnotation annotation = AnnotationDeclarationParser.parseEmbeddedAnnotation(context, nestedDeclaration);
					if (annotation != null) {
						collected.add(annotation);
					}
				}
				return collected;
			}
			ASTAnnotation annotation = AnnotationDeclarationParser.parseEmbeddedAnnotation(context, singleAnnotation);
			if (annotation != null) {
				collected.add(annotation);
			}
			return collected;
		}

		ASTArray annotations;
		if (annotationList.type() == ElementType.EMPTY) {
			annotations = ASTEmpty.EMPTY_ARRAY;
		} else if (annotationList.type() == ElementType.ARRAY) {
			annotations = (ASTArray) annotationList;
		} else {
			context.throwError(DiagnosticCode.PAYLOAD_SHAPE, "Expected parameter annotation list array", annotationList.location());
			return collected;
		}

		for (ASTElement element : annotations.values()) {
			if (!(element instanceof ASTDeclaration annotationDeclaration)) {
				context.throwError(DiagnosticCode.PAYLOAD_SHAPE,
						"Expected parameter annotation declaration",
						element == null ? annotations.location() : element.location());
				continue;
			}
			ASTAnnotation annotation = AnnotationDeclarationParser.parseEmbeddedAnnotation(context, annotationDeclaration);
			if (annotation != null) {
				collected.add(annotation);
			}
		}
		return collected;
	}

	/**
	 *
	 * @param context
	 * 		Context to parse the exceptions in.
	 * @param declaration
	 * 		Declaration to parse.
	 * @param body
	 * 		Method body to parse the exceptions from.
	 *
	 * @return List of declared exceptions.
	 */
	private static List<ASTIdentifier> parseDeclaredExceptions(ProcessorContext context, ASTDeclaration declaration,
	                                                           ASTObject body) {
		ASTElement throwsElement = body.values().get(ProcessorKeywords.THROWS);
		if (throwsElement == null)
			return Collections.emptyList();

		ASTArray array = context.validateEmptyableElement(
				throwsElement, ElementType.ARRAY, "method throws declarations", declaration
		);
		if (array == null)
			return Collections.emptyList();

		List<ASTIdentifier> types = context.validateArray(array, ElementType.IDENTIFIER, "declared exception type", declaration);
		return types.stream()
				.filter(type -> !context.isNotDescriptor(type, DescriptorForm.INTERNAL_NAME, "declared exception name"))
				.toList();
	}

	/**
	 * @param context
	 * 		Context to parse the exceptions in.
	 * @param declaration
	 * 		Declaration to parse.
	 * @param body
	 * 		Method body to parse the exceptions from.
	 *
	 * @return List of parsed exceptions. If the exceptions list is invalid, an empty list is returned and errors are reported to the context.
	 */
	private static List<ASTException> parseExceptions(ProcessorContext context, ASTDeclaration declaration, ASTObject body) {
		ASTElement exceptionsElement = body.values().get(ProcessorKeywords.EXCEPTIONS);
		if (exceptionsElement == null) {
			return new ArrayList<>();
		}

		ASTArray array = context.validateEmptyableElement(
				exceptionsElement, ElementType.ARRAY, "method exceptions", declaration
		);
		if (array == null) {
			return new ArrayList<>();
		}

		List<ASTException> exceptions = new ArrayList<>();
		for (ASTElement element : array.values()) {
			ASTArray exceptionArray = context.validateEmptyableElement(
					element, ElementType.ARRAY, "method exception", declaration
			);
			if (exceptionArray == null) {
				continue;
			}
			ASTException exception = parseException(context, exceptionArray);
			if (exception != null) {
				exceptions.add(exception);
			}
		}
		return exceptions;
	}

	/**
	 * @param context
	 * 		Context to parse the exception in.
	 * @param array
	 * 		Array to parse the exception from.
	 * 		The array is expected to have exactly 4 elements:
	 * 		<ul>
	 * 		<li>Start  label (identifier)</li>
	 * 		<li>End label (identifier)</li>
	 * 		<li>Handler label (identifier)</li>
	 * 		<li>Exception type (identifier)</li>
	 * 		</ul>
	 *
	 * @return Parsed exception, or {@code null} if the array is invalid. Errors are reported to the context.
	 */
	private static ASTException parseException(ProcessorContext context, ASTArray array) {
		ASTIdentifier start = context.validateIdentifier(array.value(0), "exception start", array);
		ASTIdentifier end = context.validateIdentifier(array.value(1), "exception end", array);
		ASTIdentifier handler = context.validateIdentifier(array.value(2), "exception handler", array);
		ASTIdentifier type = context.validateDescriptor(array.value(3), DescriptorForm.CLASS_TYPE, "exception handler type", array);
		if (start == null || end == null || handler == null || type == null)
			return null;
		return new ASTException(start, end, handler, type);
	}
}
