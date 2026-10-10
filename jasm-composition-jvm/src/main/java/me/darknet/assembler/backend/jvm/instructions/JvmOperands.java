package me.darknet.assembler.backend.jvm.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.descriptor.DescriptorForm;
import me.darknet.assembler.descriptor.PrimitiveType;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.instructions.HandleOperands;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.OperandValue;
import me.darknet.assembler.instructions.OperandValueResolver;
import me.darknet.assembler.instructions.OperandValues;
import me.darknet.assembler.instructions.Operands;
import me.darknet.assembler.instructions.PayloadSchema;
import me.darknet.assembler.instructions.SwitchKey;
import me.darknet.assembler.parser.processor.ProcessorContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operand schemas that validate and resolve JVM instruction operands.
 */
public enum JvmOperands implements Operands {
	CONSTANT(JvmOperands::verifyConstant),
	WIDE_CONSTANT(JvmOperands::verifyWideConstant),
	TABLE_SWITCH(JvmOperands::validateTableSwitch, JvmOperands::resolveTableSwitch),
	LOOKUP_SWITCH(JvmOperands::validateLookupSwitch, JvmOperands::resolveLookupSwitch),
	HANDLE(HandleOperands.HANDLE),
	ARGS(JvmOperands::verifyArgs),
	TYPE(((context, element) -> context.validateDescriptor(element, DescriptorForm.TYPE_REFERENCE, "type", element))),
	CLASS_TYPE(((context, element) -> context.validateDescriptor(element, DescriptorForm.CLASS_TYPE, "class type", element))),
	NEW_ARRAY_TYPE(JvmOperands::verifyNewArrayType);

	private static final PayloadSchema TABLE_SWITCH_SCHEMA = tableSwitchSchema();
	private static final PayloadSchema LOOKUP_SWITCH_SCHEMA = lookupSwitchSchema();

	private final Operand operand;

	JvmOperands(Operand.Processor operand) {
		this(operand, null);
	}

	JvmOperands(Operand.Processor operand, OperandValueResolver resolver) {
		this(new Operand(operand, resolver));
	}

	JvmOperands(Operand operand) {
		this.operand = operand;
	}

	@Override
	public Operand getOperand() {
		return operand;
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified table switch object.
	 *
	 * @return Payload with the case keys implied by {@code min} and the source-ordered case labels.
	 */
	private static OperandValue resolveTableSwitch(ProcessorContext context, ASTElement element) {
		ASTObject object = (ASTObject) element;
		ASTNumber min = object.value("min");
		ASTIdentifier defaultLabel = object.value("default");
		// The verifier already normalised the element shape, so this revalidates nothing it has not.
		ASTArray cases = context.validateEmptyableElement(object.value("cases"), ElementType.ARRAY, "cases", object);
		List<String> caseLabels = new ArrayList<>();
		if (cases != null) {
			for (ASTElement value : cases.values()) {
				if (value instanceof ASTIdentifier identifier)
					caseLabels.add(identifier.content());
			}
		}
		return new TableSwitchPayload(min.asInt(), defaultLabel.content(), caseLabels);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified lookup switch object.
	 *
	 * @return Payload whose keys are already sign- and radix-resolved, or {@code null} when a key is malformed.
	 */
	private static OperandValue resolveLookupSwitch(ProcessorContext context, ASTElement element) {
		ASTObject object = (ASTObject) element;
		ASTIdentifier defaultLabel = object.value("default");
		Map<Integer, String> caseLabels = new LinkedHashMap<>();
		for (var pair : object.values().pairs()) {
			if ("default".equals(pair.first().content()))
				continue;
			SwitchKey key = OperandValues.switchKey(context, pair.first());
			if (key == null)
				return null;
			ASTElement target = pair.second();
			if (target instanceof ASTIdentifier identifier)
				caseLabels.put(key.value(), identifier.content());
		}
		return new LookupSwitchPayload(defaultLabel.content(), caseLabels);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the constant.
	 */
	private static void verifyWideConstant(ProcessorContext context, ASTElement element) {
		verifyConstant(context, element);
		if (context.hasErrors())
			return;
		if (element instanceof ASTNumber number) {
			if (!number.isWide())
				context.throwUnexpectedElementError("wide constant", number);
			return;
		}
		if (element instanceof ASTIdentifier) {
			if ("nan".equalsIgnoreCase(element.content())
					|| "nand".equalsIgnoreCase(element.content())
					|| "infinity".equalsIgnoreCase(element.content())
					|| "+infinity".equalsIgnoreCase(element.content())
					|| "-infinity".equalsIgnoreCase(element.content()))
				return;
			context.throwUnexpectedElementError("wide constant", element);
			return;
		}
		context.throwUnexpectedElementError("wide constant", element);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the payload.
	 */
	private static void validateTableSwitch(ProcessorContext context, ASTElement element) {
		ASTObject object = TABLE_SWITCH_SCHEMA.validate(context, element);
		if (object == null)
			return;

		// The bounds are keys the encoding needs as integers, which is a relation between the two values
		// rather than a per-key shape.
		ASTNumber min = object.value("min");
		ASTNumber max = object.value("max");
		if (min.isFloatingPoint())
			context.throwUnexpectedElementError("integer literal", min);
		if (max.isFloatingPoint())
			context.throwUnexpectedElementError("integer literal", max);

		// The case keys are implied by the case count, so 'max' is only consistent when it says where the
		// last one lands. The emitter derives its own bounds from the labels, which is why a wrong 'max'
		// used to compile and then quietly disappear from the output.
		ASTArray array = object.value("cases");
		int expectedMax = min.asInt() + array.values().size() - 1;
		if (max.asInt() != expectedMax) {
			context.throwError(
					"Table switch max must be min + cases - 1 (" + expectedMax + ")", max.location());
		}
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the payload.
	 */
	private static void validateLookupSwitch(ProcessorContext context, ASTElement element) {
		LOOKUP_SWITCH_SCHEMA.validate(context, element);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the args array.
	 */
	private static void verifyArgs(ProcessorContext context, ASTElement element) {
		ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "args", element);
		if (array == null)
			return;
		for (ASTElement value : array.values()) {
			if (context.isNull(value, "args element", array.location()))
				continue;
			JvmOperands.verifyConstant(context, value);
		}
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the new array type.
	 */
	private static void verifyNewArrayType(ProcessorContext context, ASTElement element) {
		if (context.isNotType(element, ElementType.IDENTIFIER, "new array type"))
			return;

		PrimitiveType primitive = PrimitiveType.fromJavaName(element.content());
		if (primitive == null || primitive.isVoid())
			context.throwUnexpectedElementError("boolean, byte, char, short, int, float, long or double", element);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the constant.
	 */
	private static void verifyConstant(ProcessorContext context, ASTElement element) {
		switch (element.type()) {
			case NUMBER -> {
				ASTNumber number = (ASTNumber) element;
				// attempt to parse it
				try {
					number.number();
				} catch (NumberFormatException e) {
					context.throwIllegalArgumentStateError("not a valid number literal", number);
				}
			}
			case STRING, CHARACTER -> {
			}
			case IDENTIFIER -> {
				String content = element.content();
				char first = content.charAt(0);
				switch (first) {
					case 'L' -> {
						// A trailing ';' means a class descriptor; anything else can only be a short handle.
						if (content.charAt(content.length() - 1) == ';') {
							context.validateDescriptor(element, DescriptorForm.FIELD, "class, method or array descriptor", element);
						} else if (Handle.HANDLE_SHORTCUTS.get(content) == null) {
							context.throwUnexpectedElementError("class, method or array descriptor", element);
						}
					}
					case '[' ->
							context.validateDescriptor(element, DescriptorForm.FIELD, "class, method or array descriptor", element);
					case '(' ->
							context.validateDescriptor(element, DescriptorForm.METHOD, "class, method or array descriptor", element);
					default -> {
						// Try the special numeric spellings before treating the identifier as a handle.
						switch (content.toLowerCase()) {
							case "nan", "nand", "nanf",
							     "infinity", "+infinity", "-infinity",
							     "infinityd", "+infinityd", "-infinityd",
							     "infinityf", "+infinityf", "-infinityf" -> {
							}
							default -> {
								// Otherwise try the predefined short-handle spellings.
								Handle handle = Handle.HANDLE_SHORTCUTS.get(content);
								if (handle == null) {
									context.throwUnexpectedElementError("class, method or array descriptor", element);
								}
							}
						}
					}
				}
			}
			case ARRAY -> {
				ASTArray array = (ASTArray) element;
				if (array.values().isEmpty()) {
					context.throwUnexpectedElementError("constant", element);
					return;
				}
				ASTElement last = array.values().getLast();
				if (last == null) {
					context.throwUnexpectedElementError("constant", element);
					return;
				}
				switch (last.type()) {
					case ARRAY, EMPTY -> verifyConstantDynamic(context, array);
					case IDENTIFIER -> HandleOperands.verify(context, array);
					default -> context.throwUnexpectedElementError("constant", element);
				}
			}
			default -> context.throwUnexpectedElementError("constant", element);
		}
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param array
	 * 		Source array for the constant dynamic.
	 */
	private static void verifyConstantDynamic(ProcessorContext context, ASTArray array) {
		// constant dynamic structure: { name, type, { <handle > }, { <args> } }
		if (array.values().size() != 4) {
			context.throwUnexpectedElementError("name, type, handle and args", array);
			return;
		}

		ASTIdentifier identifier = context.validateIdentifier(array.value(0), "name", array);
		if (identifier == null)
			return;

		ASTIdentifier type = context.validateIdentifier(array.value(1), "type", array);
		if (type == null)
			return;

		if (context.isNotDescriptor(type, DescriptorForm.FIELD, "valid field descriptor"))
			return;

		if (HandleOperands.verifyAndReport(context, array.value(2)))
			return;

		ASTElement argsElement = array.value(3);

		ASTArray args = context.validateEmptyableElement(argsElement, ElementType.ARRAY, "args", array);
		if (args == null)
			return;
		for (ASTElement value : args.values()) {
			if (context.isNull(value, "args element", args.location()))
				continue;
			verifyConstant(context, value);
		}
	}

	/**
	 * @return Declared shape of a table switch payload: two bounds, a default label and the case labels.
	 */
	private static PayloadSchema tableSwitchSchema() {
		return PayloadSchema.fixed("table switch",
				PayloadSchema.Field.value("min", ElementType.NUMBER, "number"),
				PayloadSchema.Field.value("max", ElementType.NUMBER, "number"),
				PayloadSchema.Field.value("default", ElementType.IDENTIFIER, "identifier"),
				PayloadSchema.Field.arrayOf("cases", "cases", ElementType.IDENTIFIER, "label"));
	}

	/**
	 * @return Declared shape of a lookup switch payload: a required default label plus integer case keys.
	 */
	private static PayloadSchema lookupSwitchSchema() {
		return PayloadSchema.keyed("lookup switch",
				new PayloadSchema.CaseKeys(ElementType.IDENTIFIER, "identifier"),
				PayloadSchema.Field.value("default", ElementType.IDENTIFIER, "identifier"));
	}
}
