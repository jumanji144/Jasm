package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTLiteral;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.Stateful;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Context for processing an AST.
 */
public class ProcessorContext extends Stateful<ProcessorFlag> {
	private final ErrorCollector errorCollector = new ErrorCollector();
	private final BytecodeFormat format;
	private final Instructions<?> instructions;
	private final DeclarationRegistry registry;
	private ProcessingState state = new ProcessingState();

	/**
	 * @param format
	 * 		Format to process the AST for.
	 * @param registry
	 * 		Registry of declaration handlers to use when processing the AST.
	 */
	public ProcessorContext(BytecodeFormat format, DeclarationRegistry registry) {
		this.format = format;
		this.instructions = format.getInstructions();
		this.registry = registry;
	}

	/**
	 * @param element
	 * 		Element to add to the result of processing the AST.
	 */
	void add(@NotNull ASTElement element) {
		state.add(element);
	}

	/**
	 * @return List of processed AST elements, in the order they were added to the context.
	 */
	List<ASTElement> getResult() {
		return state.getResult();
	}

	/**
	 * @return List of errors that occurred during processing the AST, in the order they were added to the context.
	 */
	List<Error> getErrors() {
		return errorCollector.getErrors();
	}

	/**
	 * @return List of warnings that occurred during processing the AST, in the order they were added to the context.
	 */
	List<Warn> getWarns() {
		return errorCollector.getWarns();
	}

	/**
	 * @param nextState
	 * 		State to swap to.
	 */
	void swapState(ProcessingState nextState) {
		state = nextState;
	}

	/**
	 * @return Current processing state.
	 */
	ProcessingState state() {
		return state;
	}

	/**
	 * Parse the given declaration with recognized {@link DeclarationHandler}s registered in the {@link DeclarationRegistry}.
	 *
	 * @param declaration
	 * 		Declaration to parse.
	 *
	 * @return Parsed declaration, or {@code null} if the declaration is invalid or if there is no handler for the declaration's keyword.
	 */
	ASTElement parseDeclaration(ASTDeclaration declaration) {
		return registry.get(declaration).parse(this, declaration);
	}

	/**
	 * Add an error with the given message and location to the context.
	 *
	 * @param message
	 * 		Error message.
	 * @param location
	 * 		Location of the error.
	 */
	public void throwError(String message, Location location) {
		errorCollector.addError(new Error(message, location));
	}

	/**
	 * Add a warning with the given message and location to the context.
	 *
	 * @param value
	 * 		Value to check for nullity, used in the warning message.
	 * @param expected
	 * 		Description of the expected value, used in the warning message.
	 * @param location
	 * 		Location of the value, used in the warning message.
	 *
	 * @return {@code true} if the value is {@code null}, {@code false} otherwise.
	 */
	public boolean isNull(Object value, String expected, Location location) {
		if (value == null) {
			throwError("Expected " + expected + " but got nothing", location);
			return true;
		}
		return false;
	}

	/**
	 * Check if the given element is not of the expected type, and if so, add an error to the context.
	 *
	 * @param element
	 * 		Element to check the type of.
	 * @param type
	 * 		Expected type of the element.
	 * @param expected
	 * 		Description of the expected element, used in the error message.
	 *
	 * @return {@code true} if the element is not of the expected type, {@code false} otherwise.
	 */
	public boolean isNotType(ASTElement element, ElementType type, String expected) {
		if (element.type() != type) {
			throwUnexpectedElementError(expected, element);
			return true;
		}
		return false;
	}

	/**
	 * Check if the given element is not of the expected type, and if so, add an error to the context.
	 *
	 * @param element
	 * 		Element to check the type of.
	 * @param expectedElementType
	 * 		Expected type of the element.
	 * @param description
	 * 		Description of the expected element, used in the error message.
	 * @param parent
	 * 		Parent element of the element to check, used to get the location for the error message.
	 *
	 * @return {@code true} if the element is {@code null} or not of the expected type, {@code false} otherwise.
	 */
	public boolean validateCorrect(ASTElement element, ElementType expectedElementType, String description,
	                               ASTElement parent) {
		if (isNull(element, description, parent.location()))
			return true;
		return isNotType(element, expectedElementType, description);
	}

	/**
	 * Check if the given element is of the expected type, and if so, return it cast to the expected type.
	 * If the element is {@code null} or not of the expected type, add an error to the context and return {@code null}.
	 *
	 * @param element
	 * 		Element to check and cast.
	 * @param expectedElementType
	 * 		Expected type of the element.
	 * @param description
	 * 		Description of the expected element, used in the error message.
	 * @param parent
	 * 		Parent element of the element to check, used to get the location for the error message.
	 * @param <T>
	 * 		Expected type of the element.
	 *
	 * @return The given element cast to the expected type, or {@code null} if the element is {@code null} or not of the expected type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T validateElement(ASTElement element, ElementType expectedElementType, String description,
	                             ASTElement parent) {
		if (isNull(element, description, parent.location()))
			return null;
		if (isNotType(element, expectedElementType, description))
			return null;
		return (T) element;
	}

	/**
	 * Check if the given element is of the expected type or is an empty element,
	 * and if so, return it cast to the expected type or the corresponding empty element.
	 *
	 * @param element
	 * 		Element to check and cast.
	 * @param expectedElementType
	 * 		Expected type of the element.
	 * @param description
	 * 		Description of the expected element, used in the error message.
	 * @param parent
	 * 		Parent element of the element to check, used to get the location for the error message.
	 * @param <T>
	 * 		Expected type of the element.
	 *
	 * @return The given element cast to the expected type, the corresponding empty element if the given element is an empty element,
	 * or {@code null} if the given element is {@code null} or not of the expected type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T validateEmptyableElement(ASTElement element, ElementType expectedElementType, String description,
	                                      ASTElement parent) {
		if (isNull(element, description, parent.location()))
			return null;
		if (element.type() == ElementType.EMPTY) {
			return (T) switch (expectedElementType) {
				case OBJECT -> ASTEmpty.EMPTY_OBJECT;
				case ARRAY -> ASTEmpty.EMPTY_ARRAY;
				case CODE -> ASTEmpty.EMPTY_CODE;
				case DECLARATION -> ASTEmpty.EMPTY_DECLARATION;
				default -> {
					throwUnexpectedElementError(description, element);
					yield null;
				}
			};
		}
		if (isNotType(element, expectedElementType, description))
			return null;
		return (T) element;
	}

	/**
	 * Check if the given element is an array of the expected element type,
	 * and if so, return a list of the elements in the array cast to the expected type.
	 *
	 * @param array
	 * 		Element to check and cast.
	 * @param expectedElements
	 * 		Expected type of the elements in the array.
	 * @param description
	 * 		Description of the expected element, used in the error message.
	 * @param parent
	 * 		Parent element of the element to check, used to get the location for the error message.
	 * @param <T>
	 * 		Expected type of the elements in the array.
	 *
	 * @return List of the elements in the array cast to the expected type,
	 * or an empty list if the given element is {@code null} or not an array of the expected element type.
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> validateArray(ASTArray array, ElementType expectedElements, String description,
	                                 ASTElement parent) {
		if (isNull(array, description, parent.location())) {
			return Collections.emptyList();
		}
		List<T> result = new ArrayList<>();
		for (ASTElement element : array.values()) {
			if (isNull(element, description, parent.location()))
				continue;
			if (isNotType(element, expectedElements, description))
				continue;
			result.add((T) element);
		}
		return result;
	}

	/**
	 * Check if the given element is an object with the expected keys,
	 *
	 * @param element
	 * 		Element to check.
	 * @param description
	 * 		Description of the expected element, used in the error message.
	 * @param parent
	 * 		Parent element of the element to check, used to get the location for the error message.
	 * @param expectedKeys
	 * 		Expected keys in the object, used in the error message.
	 *
	 * @return The given element cast to an object if it is an object with the expected keys, or {@code null} otherwise.
	 */
	public ASTObject validateObject(ASTElement element, String description, ASTElement parent, String... expectedKeys) {
		if (isNull(element, description, parent.location()))
			return null;
		if (isNotType(element, ElementType.OBJECT, description))
			return null;
		ASTObject object = (ASTObject) element;
		if (object.values().size() != expectedKeys.length)
			throwError("Expected " + expectedKeys.length + " keys in " + description, object.location());
		for (String expectedKey : expectedKeys) {
			if (!object.values().containsKey(expectedKey)) {
				throwError("Expected key '" + expectedKey + "' in " + description, object.location());
				return null;
			}
		}
		return object;
	}

	/**
	 * Check if the given element is an identifier or a number, and if so, return it as an identifier.
	 *
	 * @param element
	 * 		Element to check and cast.
	 * @param description
	 * 		Description of the expected element, used in the error message.
	 * @param parent
	 * 		Parent element of the element to check, used to get the location for the error message.
	 *
	 * @return The given element as an identifier if it is an identifier or a number,
	 * or {@code null} if the given element is {@code null} or not an identifier or a number.
	 */
	public ASTIdentifier validateIdentifier(ASTElement element, String description, ASTElement parent) {
		if (isNull(element, description, parent.location()))
			return null;
		return validateMaybeIdentifier(element, description, parent);
	}

	/**
	 * Check if the given element is an identifier or a number, and if so, return it as an identifier.
	 *
	 * @param element
	 * 		Element to check and cast.
	 * @param description
	 * 		Description of the expected element, used in the error message.
	 * @param parent
	 * 		Parent element of the element to check, used to get the location for the error message.
	 *
	 * @return The given element as an identifier if it is an identifier or a number,
	 * or {@code null} if the given element is not an identifier or a number.
	 */
	public ASTIdentifier validateMaybeIdentifier(ASTElement element, String description, ASTElement parent) {
		if (element == null)
			return null;

		if (element.type() == ElementType.NUMBER)
			return new ASTIdentifier(element.value());

		if (!(element instanceof ASTLiteral)) {
			throwUnexpectedElementError(description, element);
			return null;
		}
		return (ASTIdentifier) element;
	}

	/**
	 * Parse the given list of elements as declarations with recognized
	 * {@link DeclarationHandler}s registered in the {@link DeclarationRegistry}.
	 *
	 * @param elements
	 * 		List of elements to parse as declarations.
	 * @param expected
	 * 		Description of the expected declarations, used in the error message.
	 * @param parent
	 * 		Parent element of the elements to parse, used to get the location for the error message.
	 * @param types
	 * 		Expected types of the declarations, used to get the appropriate handler from the registry and in the error message.
	 *
	 * @return List of parsed declarations, in the order they were parsed.
	 * Declarations that are invalid or have no handler in the registry are skipped and not included in the result.
	 */
	List<ASTElement> parseDeclarations(List<ASTElement> elements, String expected, Location parent, String... types) {
		ProcessingState outerState = state;
		ProcessingState nestedState = new ProcessingState();
		swapState(nestedState);
		Location lastLocation = parent;
		for (ASTElement element : elements) {
			if (isNull(element, expected, lastLocation))
				continue;

			lastLocation = element.location();
			if (isNotType(element, ElementType.DECLARATION, expected))
				continue;

			ASTDeclaration declaration = (ASTDeclaration) element;
			String keyword = DeclarationRegistry.keyword(declaration);
			boolean found = false;
			for (String type : types) {
				if (keyword.equals(type)) {
					found = true;
					break;
				}
			}
			if (!found) {
				throwUnexpectedElementError(expected, element);
				continue;
			}
			ASTElement parsedDeclaration = parseDeclaration(declaration);
			if (parsedDeclaration != null) {
				nestedState.add(parsedDeclaration);
			}
		}
		swapState(outerState);
		return nestedState.getResult();
	}

	/**
	 * Get the element at the given index in the given declaration, or {@code null} if the index is out of bounds.
	 *
	 * @param declaration
	 * 		Declaration to get the element from.
	 * @param index
	 * 		Index of the element to get.
	 *
	 * @return Element at the given index in the given declaration, or {@code null} if the index is out of bounds.
	 */
	ASTElement declarationElement(ASTDeclaration declaration, int index) {
		return index >= 0 && index < declaration.elements().size() ? declaration.element(index) : null;
	}

	/**
	 * Add an error to the context indicating that an unexpected element was found.
	 *
	 * @param expected
	 * 		Description of the expected element, used in the error message.
	 * @param actual
	 * 		Element that was actually found, used in the error message.
	 */
	public void throwUnexpectedElementError(String expected, ASTElement actual) {
		throwError(
				"Expected " + expected + " but got " + actual.type().name().toLowerCase() + " '" + actual.content() + "'",
				actual.location()
		);
	}

	/**
	 * Add an error to the context indicating that an element is in an illegal state.
	 *
	 * @param state
	 * 		Description of the illegal state, used in the error message.
	 * @param actual
	 * 		Element that is in the illegal state, used in the error message.
	 */
	public void throwIllegalArgumentStateError(String state, ASTElement actual) {
		throwError(actual.type().name().toLowerCase() + " '" + actual.content() + "' is " + state, actual.location());
	}

	/**
	 * @return Instructions for the bytecode format this context is processing the AST for.
	 */
	public Instructions<?> getInstructions() {
		return instructions;
	}

	/**
	 * @return Bytecode format this context is processing the AST for.
	 */
	public BytecodeFormat getFormat() {
		return format;
	}

}
