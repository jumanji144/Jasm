package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTException;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.visitor.Modifiers;

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
		ASTElement parametersElement = body.values().get("parameters");
		if (parametersElement != null) {
			ASTArray parameterArray = context.validateEmptyableElement(
					parametersElement, ElementType.ARRAY, "method parameters", declaration
			);
			if (parameterArray != null) {
				parameters = context.validateArray(parameterArray, ElementType.IDENTIFIER, "method parameter", declaration);
			}
		}

		Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = parseParameterAnnotations(context, declaration, body);

		ASTElement defaultValueElement = body.values().get("default-value");
		if (defaultValueElement != null) {
			context.enterState(ProcessorFlag.IN_ANNOTATION);
			AnnotationDeclarationParser.validateElementValue(context, defaultValueElement);
			context.leaveState(ProcessorFlag.IN_ANNOTATION);
		}

		List<ASTException> exceptions = parseExceptions(context, declaration, body);

		ASTCode code = null;
		List<Instruction<?>> instructions = new ArrayList<>();
		ASTElement codeElement = body.values().get("code");
		if (codeElement != null) {
			code = context.validateEmptyableElement(codeElement, ElementType.CODE, "method code", declaration);
			if (code != null) {
				for (ASTInstruction instruction : code.instructions()) {
					if (instruction == null || instruction instanceof ASTLabel) {
						continue;
					}
					Instruction<?> parsed = context.getInstructions().get(instruction.identifier().content());
					if (parsed == null) {
						context.throwError(
								"Unknown instruction: " + instruction.identifier().content(),
								instruction.identifier().location()
						);
						continue;
					}
					parsed.verify(instruction, context);
					instructions.add(parsed);
				}
			}
		}

		int nameIndex = lastIndex - 2;
		int descIndex = lastIndex - 1;
		ASTIdentifier name = context.validateIdentifier(
				context.declarationElement(declaration, nameIndex), "method name", declaration
		);
		ASTIdentifier desc = context.validateIdentifier(
				context.declarationElement(declaration, descIndex), "method descriptor", declaration
		);
		if (name == null || desc == null) {
			return null;
		}

		Modifiers modifiers = ModifierParser.parseModifiers(context, nameIndex, declaration);
		return new ASTMethod(
				modifiers,
				name,
				desc,
				parameters,
				parameterAnnotations,
				defaultValueElement,
				exceptions,
				code,
				instructions,
				context.getFormat()
		).accept(context.state().collectAttributes());
	}

	/**
	 * @param context
	 * 		Context to parse the parameter annotations in.
	 * @param declaration
	 * 		Declaration to report errors on.
	 * @param body
	 * 		Method body to parse the parameter annotations from.
	 *
	 * @return Map of parameter names to their annotations.
	 * Parameters without annotations are not included in the map.
	 */
	private static Map<ASTIdentifier, List<ASTAnnotation>> parseParameterAnnotations(ProcessorContext context,
	                                                                                 ASTDeclaration declaration, ASTObject body) {
		ASTElement parameterAnnotationsElement = body.values().get("parameter-annotations");
		if (parameterAnnotationsElement == null) {
			return new IdentityHashMap<>();
		}

		ASTObject parameterAnnotationsObject = context.validateEmptyableElement(
				parameterAnnotationsElement,
				ElementType.OBJECT,
				"parameter annotations",
				declaration
		);
		if (parameterAnnotationsObject == null) {
			return new IdentityHashMap<>();
		}

		Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = new IdentityHashMap<>();
		context.enterState(ProcessorFlag.SKIP_PENDING_ANNOTATION);
		for (var pair : parameterAnnotationsObject.values().pairs()) {
			ASTIdentifier parameter = context.validateMaybeIdentifier(
					pair.first(), "parameter annotation target", declaration
			);
			if (parameter == null) {
				continue;
			}

			List<ASTAnnotation> collected = collectParameterAnnotations(context, declaration, pair.second());
			if (!collected.isEmpty()) {
				parameterAnnotations.put(parameter, collected);
			}
		}
		context.leaveState(ProcessorFlag.SKIP_PENDING_ANNOTATION);
		return parameterAnnotations;
	}

	/**
	 * @param context
	 * 		Context to parse the parameter annotations in.
	 * @param declaration
	 * 		Declaration to report errors on.
	 * @param annotationList
	 * 		List of annotations to parse.
	 * 		This can be either a single annotation or an array of annotations.
	 *
	 * @return List of parsed annotations. If the annotation list is invalid, an empty list is returned and errors are reported to the context.
	 */
	private static List<ASTAnnotation> collectParameterAnnotations(ProcessorContext context, ASTDeclaration declaration,
	                                                               ASTElement annotationList) {
		List<ASTAnnotation> collected = new ArrayList<>();
		if (annotationList instanceof ASTDeclaration singleAnnotation) {
			if (singleAnnotation.keyword() == null) {
				for (ASTElement element : singleAnnotation.elements()) {
					ASTDeclaration nestedDeclaration = context.validateElement(
							element, ElementType.DECLARATION, "parameter annotation", declaration
					);
					if (nestedDeclaration == null) {
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

		ASTArray annotations = context.validateEmptyableElement(
				annotationList, ElementType.ARRAY, "parameter annotation list", declaration
		);
		if (annotations == null) {
			return collected;
		}

		for (ASTElement element : annotations.values()) {
			ASTDeclaration annotationDeclaration = context.validateElement(
					element, ElementType.DECLARATION, "parameter annotation", declaration
			);
			if (annotationDeclaration == null) {
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
	 * @param context
	 * 		Context to parse the exceptions in.
	 * @param declaration
	 * 		Declaration to report errors on.
	 * @param body
	 * 		Method body to parse the exceptions from.
	 *
	 * @return List of parsed exceptions. If the exceptions list is invalid, an empty list is returned and errors are reported to the context.
	 */
	private static List<ASTException> parseExceptions(ProcessorContext context, ASTDeclaration declaration, ASTObject body) {
		ASTElement exceptionsElement = body.values().get("exceptions");
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
		ASTIdentifier type = context.validateIdentifier(array.value(3), "exception type", array);
		if (start == null || end == null || handler == null || type == null)
			return null;
		return new ASTException(start, end, handler, type);
	}
}
