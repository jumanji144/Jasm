package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.parser.processor.ProcessorContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Declared shape of an object-spelled instruction payload.
 * <p>
 * Replaces the object checks each operand verifier used to repeat by hand: a payload declares the
 * keys it accepts, what each accepts, and whether keys beyond those are case keys. Turning a payload
 * into a value stays the resolver's job, so a schema never has to know how a payload is encoded.
 *
 * @param description
 * 		Human-readable payload name used in diagnostics.
 * @param fields
 * 		Keys with a fixed meaning, in the order diagnostics report them.
 * @param caseKeys
 * 		How keys beyond {@code fields} are read, or {@code null} when the payload's key set is fixed.
 */
public record PayloadSchema(@NotNull String description,
                            @NotNull List<Field> fields,
                            @Nullable CaseKeys caseKeys) {
	/**
	 * @return Schema for a payload with a fixed key set.
	 */
	public static @NotNull PayloadSchema fixed(@NotNull String description, @NotNull Field... fields) {
		return new PayloadSchema(description, List.of(fields), null);
	}

	/**
	 * @return Schema for a payload whose remaining keys are case keys.
	 */
	public static @NotNull PayloadSchema keyed(@NotNull String description, @NotNull CaseKeys caseKeys,
	                                           @NotNull Field... fields) {
		return new PayloadSchema(description, List.of(fields), caseKeys);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the payload.
	 *
	 * @return Verified payload object, or {@code null} after reporting why it is not one.
	 */
	public @Nullable ASTObject validate(@NotNull ProcessorContext context, @NotNull ASTElement element) {
		if (element.type() != ElementType.OBJECT) {
			context.throwError(DiagnosticCode.PAYLOAD_SHAPE,
					"Expected " + description + " object but got " + element.type().name().toLowerCase(),
					element.location());
			return null;
		}
		ASTObject object = (ASTObject) element;

		if (caseKeys == null) {
			object = context.validateObject(element, description, element,
					fields.stream().map(Field::key).toArray(String[]::new));
			if (object == null || object.values().size() != fields.size())
				return null;
		} else {
			for (Field field : fields) {
				if (!field.required() || object.values().containsKey(field.key()))
					continue;
				context.throwError(DiagnosticCode.PAYLOAD_SHAPE, "Expected key '" + field.key() + "' in " + description, object.location());
				return null;
			}
		}

		for (Field field : fields) {
			ASTElement value = object.value(field.key());
			if (value == null) {
				if (field.required()) {
					context.throwError(DiagnosticCode.PAYLOAD_SHAPE, "Expected " + field.expected(), object.location());
					return null;
				}
				continue;
			}

			if (field.elementType() == null) {
				if (context.validateCorrect(value, field.type(), field.expected(), object))
					return null;
				continue;
			}

			ASTArray array = context.validateEmptyableElement(value, ElementType.ARRAY, field.expected(), object);
			if (array == null)
				return null;
			List<?> values = context.validateArray(array, field.elementType(), field.elementExpected(), object);
			if (values.size() != array.values().size())
				return null;
		}

		if (caseKeys != null && !validateCaseKeys(context, object))
			return null;
		return object;
	}

	private boolean validateCaseKeys(@NotNull ProcessorContext context, @NotNull ASTObject object) {
		CaseKeys keys = caseKeys;
		if (keys == null)
			return true;
		for (var pair : object.values().pairs()) {
			ASTElement key = pair.first();
			if (key == null) {
				context.isNull(null, "switch key", object.location());
				return false;
			}
			if (fields.stream().anyMatch(field -> field.key().equals(key.content())))
				continue;
			if (OperandValues.switchKey(context, key) == null)
				return false;
			ASTElement value = pair.second();
			if (value == null) {
				context.isNull(null, keys.expectedValue(), object.location());
				return false;
			}
			if (context.validateCorrect(value, keys.valueType(), keys.expectedValue(), object))
				return false;
		}
		return true;
	}

	/**
	 * Declared meaning of a payload key.
	 *
	 * @param key
	 * 		Declared key name.
	 * @param type
	 * 		Expected value type.
	 * @param expected
	 * 		Human-readable description of the expected value type used in diagnostics.
	 * @param elementType
	 * 		Expected array element type, or {@code null} if the field is not an array.
	 * @param elementExpected
	 * 		Human-readable description of the expected array element type used in diagnostics, or {@code null}
	 * 		if the field is not an array.
	 * @param required
	 * 		Whether the field must be present in a payload object.
	 */
	public record Field(@NotNull String key, @NotNull ElementType type, @NotNull String expected,
	                    @Nullable ElementType elementType, @Nullable String elementExpected, boolean required) {

		/**
		 * @return Required field accepting any element of {@code type}.
		 */
		public static @NotNull Field value(@NotNull String key, @NotNull ElementType type,
		                                   @NotNull String expected) {
			return new Field(key, type, expected, null, null, true);
		}

		/**
		 * @return Required array field whose entries are checked against {@code elementType}.
		 */
		public static @NotNull Field arrayOf(@NotNull String key, @NotNull String expected,
		                                     @NotNull ElementType elementType, @NotNull String elementExpected) {
			return new Field(key, ElementType.ARRAY, expected, elementType, elementExpected, true);
		}
	}

	/**
	 * How a payload reads keys that its fields do not declare.
	 *
	 * @param valueType
	 * 		Type of the value for each case key.
	 * @param expectedValue
	 * 		Human-readable description of the expected value type used in diagnostics.
	 */
	public record CaseKeys(@NotNull ElementType valueType, @NotNull String expectedValue) {}
}
