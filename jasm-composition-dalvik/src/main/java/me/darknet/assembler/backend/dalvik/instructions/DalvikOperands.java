package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.Descriptor;
import me.darknet.assembler.descriptor.DescriptorForm;
import me.darknet.assembler.descriptor.PrimitiveType;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.HandleOperands;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.OperandValue;
import me.darknet.assembler.instructions.OperandValueResolver;
import me.darknet.assembler.instructions.OperandValues;
import me.darknet.assembler.instructions.Operands;
import me.darknet.assembler.instructions.PayloadSchema;
import me.darknet.assembler.instructions.SwitchKey;
import me.darknet.assembler.parser.processor.ProcessorContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operand schemas that validate and resolve Dalvik instruction operands.
 */
public enum DalvikOperands implements Operands {
	CONSTANT(DalvikOperands::verifyConstant),
	REGISTER(DefaultOperands.LITERAL.getOperand().verifier(), DalvikOperands::resolveRegister),
	REGISTER_WIDE(DefaultOperands.LITERAL.getOperand().verifier(), DalvikOperands::resolveRegisterWide),
	REGISTER_ARRAY(DalvikOperands::verifyRegisterArray, DalvikOperands::resolveRegisterList),
	REGISTER_RANGE(DalvikOperands::verifyRegisterRange, DalvikOperands::resolveRegisterRange),
	LITERAL_8(DalvikOperands::verifyLiteral8, DalvikOperands::resolveLiteral8),
	LITERAL_16(DalvikOperands::verifyLiteral16, DalvikOperands::resolveLiteral16),
	CLASS_TYPE(DalvikOperands::verifyClassType),
	METHOD_TYPE(DalvikOperands::verifyMethodType),
	FILLED_NEW_ARRAY_TYPE(DalvikOperands::verifyFilledNewArrayType),
	HANDLE(HandleOperands.HANDLE),
	ARGS_ARRAY(DalvikOperands::verifyArgsArray),
	DATA_ARRAY(DalvikOperands::verifyDataArray, DalvikOperands::resolveArrayData),
	PACKED_SWITCH(DalvikOperands::verifyPackedSwitch, DalvikOperands::resolvePackedSwitch),
	SPARSE_SWITCH(DalvikOperands::verifySparseSwitch, DalvikOperands::resolveSparseSwitch);

	private static final PayloadSchema PACKED_SWITCH_SCHEMA = packedSwitchSchema();
	private static final PayloadSchema SPARSE_SWITCH_SCHEMA = sparseSwitchSchema();
	private static final PayloadSchema DATA_PAYLOAD_SCHEMA = dataPayloadSchema();

	/**
	 * Most registers the non-range register-list encoding can name.
	 * <p>
	 * The 35c format carries a four-bit argument count and exactly five register fields, so the format is
	 * defined only for a count of zero to five. A longer argument list is what the {@code /range} form is
	 * for, so this is a limit of the encoding rather than of a particular instruction.
	 */
	private static final int MAX_REGISTER_ARRAY = 5;

	private final Operand operand;

	DalvikOperands(Operand.Processor operand) {
		this(operand, null);
	}

	DalvikOperands(Operand.Processor operand, OperandValueResolver resolver) {
		this(new Operand(operand, resolver));
	}

	DalvikOperands(Operand operand) {
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
	 * 		Verified register name.
	 *
	 * @return Numeric register when the name is a {@code vN} form, otherwise a named register whose
	 * slot is resolved during emission.
	 */
	private static OperandValue resolveRegister(ProcessorContext context, ASTElement element) {
		return resolveRegister(context, element, OperandRole.WidthPolicy.SINGLE);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified register name of an operand that consumes two register words.
	 *
	 * @return Register declaring the wide width its schema accepted.
	 */
	private static OperandValue resolveRegisterWide(ProcessorContext context, ASTElement element) {
		return resolveRegister(context, element, OperandRole.WidthPolicy.WIDE);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified register name.
	 * @param width
	 * 		Width declared by the operand schema.
	 *
	 * @return Numeric register when the name is a {@code vN} form, otherwise a named register whose
	 * slot is resolved during emission.
	 */
	private static OperandValue resolveRegister(ProcessorContext context, ASTElement element,
	                                            OperandRole.WidthPolicy width) {
		String name = element.content();
		Integer index = numericRegisterIndex(context, name, element);
		return new RegisterRef(name, index, width);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified literal element.
	 */
	private static void verifyLiteral8(ProcessorContext context, ASTElement element) {
		verifySignedLiteral(context, element, Byte.MIN_VALUE, Byte.MAX_VALUE, "-128 to 127");
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified literal element.
	 */
	private static void verifyLiteral16(ProcessorContext context, ASTElement element) {
		verifySignedLiteral(context, element, Short.MIN_VALUE, Short.MAX_VALUE, "-32768 to 32767");
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified 8-bit literal element.
	 *
	 * @return Literal carrying its 8-bit encoding width.
	 */
	private static OperandValue resolveLiteral8(ProcessorContext context, ASTElement element) {
		return new SignedLiteral((int) ((ASTNumber) element).asLong(), 8);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified 16-bit literal element.
	 *
	 * @return Literal carrying its 16-bit encoding width.
	 */
	private static OperandValue resolveLiteral16(ProcessorContext context, ASTElement element) {
		return new SignedLiteral((int) ((ASTNumber) element).asLong(), 16);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified class or array descriptor.
	 */
	private static void verifyClassType(ProcessorContext context, ASTElement element) {
		context.validateDescriptor(element, DescriptorForm.CLASS_OR_ARRAY, "class or array descriptor", element);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified method descriptor.
	 */
	private static void verifyMethodType(ProcessorContext context, ASTElement element) {
		context.validateDescriptor(element, DescriptorForm.METHOD, "method descriptor", element);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified array of constant elements.
	 */
	private static void verifyArgsArray(ProcessorContext context, ASTElement element) {
		// args array can be: register or array
		ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "args array", element);
		if (array == null)
			return;
		for (ASTElement value : array.values()) {
			if (context.isNull(value, "args array element", array.location()))
				continue;
			DalvikOperands.verifyConstant(context, value);
		}
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified literal element.
	 * @param minimum
	 * 		Lowest value the encoding can carry.
	 * @param maximum
	 * 		Highest value the encoding can carry.
	 * @param range
	 * 		Human-readable range used in the diagnostic.
	 */
	private static void verifySignedLiteral(ProcessorContext context, ASTElement element,
	                                        long minimum, long maximum, String range) {
		if (context.isNotType(element, ElementType.NUMBER, "integer literal"))
			return;
		ASTNumber number = (ASTNumber) element;
		if (number.isFloatingPoint()) {
			context.throwUnexpectedElementError("integer literal", number);
			return;
		}

		long value;
		try {
			value = number.asLong();
		} catch (NumberFormatException exception) {
			// A literal too large for a long cannot be encoded at any width.
			context.throwUnexpectedElementError("literal in the signed range " + range, number);
			return;
		}
		if (value < minimum || value > maximum)
			context.throwUnexpectedElementError("literal in the signed range " + range, number);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified register array.
	 *
	 * @return Registers in source order.
	 */
	private static OperandValue resolveRegisterList(ProcessorContext context, ASTElement element) {
		List<ASTElement> values = registerElements(context, element);
		if (values == null)
			return null;
		List<RegisterRef> registers = new ArrayList<>(values.size());
		for (ASTElement value : values)
			registers.add((RegisterRef) resolveRegister(context, value));
		return new RegisterList(Collections.unmodifiableList(registers));
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified two-entry register range.
	 *
	 * @return Range of the first and last register, or {@code null} after reporting reversed bounds.
	 */
	private static OperandValue resolveRegisterRange(ProcessorContext context, ASTElement element) {
		List<ASTElement> values = registerElements(context, element);
		if (values == null)
			return null;

		// The verifier rejects any other arity, so reading both bounds is only reachable at size two.
		if (values.size() != 2)
			throw new IllegalStateException("Range instructions require first and last register bounds");

		RegisterRef first = (RegisterRef) resolveRegister(context, values.get(0));
		RegisterRef last = (RegisterRef) resolveRegister(context, values.get(1));

		// When both bounds are numeric the order is checkable here; named bounds only get their slots
		// during emission, so the deferred check lives in the code visitor. The instruction is the only
		// location reported: a range is written on one line, so pointing at the individual bounds would add
		// two positions inside the expression the reader is already looking at.
		if (first.index() != null && last.index() != null && last.index() < first.index()) {
			context.throwError(DiagnosticCode.OPERAND_SHAPE, "Range instruction register bounds are reversed",
					element.location());
			return null;
		}
		return new RegisterRange(first, last);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the payload.
	 */
	private static void verifyPackedSwitch(ProcessorContext context, ASTElement element) {
		ASTObject object = PACKED_SWITCH_SCHEMA.validate(context, element);
		if (object == null)
			return;

		// The lowest key has to be an integer, which is a property of the value rather than its shape.
		ASTNumber min = object.value("first");
		if (min.isFloatingPoint())
			context.throwUnexpectedElementError("integer literal", min);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the payload.
	 */
	private static void verifySparseSwitch(ProcessorContext context, ASTElement element) {
		SPARSE_SWITCH_SCHEMA.validate(context, element);
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified packed switch object.
	 *
	 * @return Payload with the lowest key and source-ordered target labels, or {@code null} after a key error.
	 */
	private static OperandValue resolvePackedSwitch(ProcessorContext context, ASTElement element) {
		ASTObject object = (ASTObject) element;
		ASTNumber first = object.value("first");
		SwitchKey key = OperandValues.switchKey(context, first);
		if (key == null)
			return null;

		List<String> targets = new ArrayList<>();
		ASTArray array = context.validateEmptyableElement(object.value("targets"), ElementType.ARRAY, "targets", object);
		if (array != null) {
			for (ASTElement value : array.values()) {
				if (value instanceof ASTIdentifier identifier)
					targets.add(identifier.content());
			}
		}
		return new PackedSwitchPayload(key.value(), Collections.unmodifiableList(targets));
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified sparse switch object.
	 *
	 * @return Payload mapping each resolved key to its target label, or {@code null} after a key error.
	 */
	private static OperandValue resolveSparseSwitch(ProcessorContext context, ASTElement element) {
		ASTObject object = (ASTObject) element;
		Map<Integer, String> targets = new LinkedHashMap<>();
		for (var pair : object.values().pairs()) {
			SwitchKey key = OperandValues.switchKey(context, pair.first());
			if (key == null)
				return null;
			ASTElement target = pair.second();
			if (target instanceof ASTIdentifier identifier)
				targets.put(key.value(), identifier.content());
		}
		return new SparseSwitchPayload(Collections.unmodifiableMap(targets));
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified data array or payload object.
	 *
	 * @return Literals with the declared or inferred element width, or {@code null} after reporting a
	 * payload that cannot be encoded.
	 */
	private static OperandValue resolveArrayData(ProcessorContext context, ASTElement element) {
		List<ASTNumber> values;
		Integer declaredWidth = null;
		if (element instanceof ASTObject) {
			ASTObject payload = DATA_PAYLOAD_SCHEMA.validate(context, element);
			if (payload == null)
				return null;
			declaredWidth = ((ASTNumber) payload.value("width")).asInt();
			values = numbers((ASTArray) payload.value("values"));
		} else {
			// Normalise for the same reason as registers: an empty literal arrives as an empty element.
			ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "data array", element);
			if (array == null)
				return null;
			values = numbers(array);
		}

		if (declaredWidth != null)
			return new ArrayData(declaredWidth, Collections.unmodifiableList(values));

		// Without a declared width the payload has to be as wide as its widest literal, otherwise the
		// narrowest value that keeps every literal lossless.
		int elementWidth = 1;
		for (ASTNumber number : values) {
			if (DalvikArrayLiterals.isFloating(number)) {
				elementWidth = Math.max(elementWidth, number.isWide() ? Double.BYTES : Float.BYTES);
				continue;
			}

			long value;
			try {
				value = DalvikArrayLiterals.toIntegral(number);
			} catch (NumberFormatException exception) {
				context.throwError("fill-array-data requires numeric literals", number.location());
				return null;
			}
			if (number.isWide()) {
				elementWidth = Math.max(elementWidth, Long.BYTES);
			} else if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
				elementWidth = Math.max(elementWidth, Integer.BYTES);
			} else if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
				elementWidth = Math.max(elementWidth, Short.BYTES);
			}
		}
		return new ArrayData(elementWidth, Collections.unmodifiableList(values));
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Verified register array or range element.
	 *
	 * @return Register elements, or {@code null} after reporting a diagnostic. Holes the verifier
	 * already reported are skipped.
	 */
	private static @Nullable List<ASTElement> registerElements(ProcessorContext context, ASTElement element) {
		// Normalise first: an empty register literal arrives as an empty element, not an array.
		ASTArray array = registerArray(context, element);
		if (array == null)
			return null;
		List<ASTElement> values = new ArrayList<>(array.values().size());
		for (ASTElement value : array.values()) {
			if (value != null)
				values.add(value);
		}
		return values;
	}

	/**
	 * @param array
	 * 		Verified array of numeric literals.
	 *
	 * @return Numeric literals in source order.
	 */
	private static List<ASTNumber> numbers(ASTArray array) {
		List<ASTNumber> values = new ArrayList<>();
		if (array == null)
			return values;
		for (ASTElement value : array.values()) {
			if (value instanceof ASTNumber number)
				values.add(number);
		}
		return values;
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param name
	 * 		Register name to interpret.
	 * @param element
	 * 		Source element, used for diagnostic locations.
	 *
	 * @return Register index for a {@code vN} name, or {@code null} when the name is not numeric.
	 */
	private static Integer numericRegisterIndex(ProcessorContext context, String name, ASTElement element) {
		if (name.length() < 2 || name.charAt(0) != 'v') {
			return null;
		}
		for (int i = 1; i < name.length(); i++) {
			if (!Character.isDigit(name.charAt(i))) {
				return null;
			}
		}
		try {
			return Integer.parseInt(name.substring(1));
		} catch (NumberFormatException exception) {
			context.throwError("Invalid register name: " + name, element.location());
			return null;
		}
	}

	private static void verifyRegisterArray(ProcessorContext context, ASTElement element) {
		ASTArray array = registerArray(context, element);
		if (array == null)
			return;
		verifyRegisterElements(context, array);

		// The non-range register-list encoding has one register field per argument and a four-bit count, so
		// the format itself is only defined for at most five arguments. Android's verifier rejects a longer
		// list, and the writer here would run off the end of those five fields, so the limit belongs at
		// processing where the user can be told what to write instead.
		int count = array.values().size();
		if (count > MAX_REGISTER_ARRAY) {
			context.throwError(DiagnosticCode.OPERAND_SHAPE,
					"This form takes at most " + MAX_REGISTER_ARRAY + " registers but got " + count
							+ "; use the /range form for a longer argument list",
					element.location());
		}
	}

	private static void verifyRegisterRange(ProcessorContext context, ASTElement element) {
		ASTArray array = registerArray(context, element);
		if (array == null)
			return;
		verifyRegisterElements(context, array);
		// The resolver indexes both bounds, so arity has to be rejected before it runs.
		if (array.values().size() != 2)
			context.throwError("Range instructions require first and last register bounds", element.location());
	}

	/**
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Candidate register array or range element.
	 *
	 * @return The element as an array, with an empty literal normalised to an empty array, or
	 * {@code null} after reporting a diagnostic.
	 */
	private static @Nullable ASTArray registerArray(ProcessorContext context, ASTElement element) {
		return context.validateEmptyableElement(element, ElementType.ARRAY, "register array", element);
	}

	private static void verifyRegisterElements(ProcessorContext context, ASTArray array) {
		for (ASTElement value : array.values()) {
			if (context.isNull(value, "register array element", array.location()))
				continue;
			if (value.type() != ElementType.IDENTIFIER)
				context.throwUnexpectedElementError("register", value);
		}
	}

	/**
	 * Validates the array type operand of {@code filled-new-array}.
	 *
	 * @param context
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Source element for the array type.
	 */
	static void verifyFilledNewArrayType(ProcessorContext context, ASTElement element) {
		ASTIdentifier identifier = context.validateIdentifier(element, "array descriptor", element);
		if (identifier == null || context.isNotDescriptor(identifier, DescriptorForm.CLASS_OR_ARRAY, "array descriptor"))
			return;
		Descriptor parsed = me.darknet.assembler.descriptor.DescriptorParser.parse(identifier.literal(), DescriptorForm.CLASS_OR_ARRAY);

		// A class is well-formed but cannot be constructed from a register list.
		if (!(parsed instanceof ArrayDescriptor array)) {
			context.throwUnexpectedElementError("array descriptor", element);
			return;
		}

		// Only the outermost component decides the element width. A nested array is a reference, so an
		// array of double arrays is single-word and legal even though its inner element is not.
		if (array.component() instanceof PrimitiveType primitive
				&& (primitive == PrimitiveType.LONG || primitive == PrimitiveType.DOUBLE)) {
			context.throwError(DiagnosticCode.INVALID_DESCRIPTOR,
					"filled-new-array cannot build an array of " + primitive.name().toLowerCase()
							+ ": its contents must be single-word, so use new-array with fill-array-data",
					element.location());
		}
	}

	static void verifyDataArray(ProcessorContext context, ASTElement element) {
		if (element == null)
			return;

		if (element.type() == ElementType.ARRAY) {
			ASTArray array = (ASTArray) element;
			for (ASTElement value : array.values()) {
				if (context.isNull(value, "data array element", array.location())) {
					continue;
				}
				if (value.type() != ElementType.NUMBER) {
					context.throwUnexpectedElementError("number", value);
				}
			}
			return;
		}
		if (element.type() != ElementType.OBJECT) {
			context.throwUnexpectedElementError("data array or payload object", element);
			return;
		}

		ASTObject payload = DATA_PAYLOAD_SCHEMA.validate(context, element);
		if (payload == null)
			return;

		// The encoding only carries 1, 2, 4 and 8 byte elements, which is a constraint on the value
		// rather than on the payload's shape, so it stays out of the schema.
		ASTNumber width = payload.value("width");
		if (width.isFloatingPoint() || (width.asInt() != 1 && width.asInt() != 2 && width.asInt() != 4 && width.asInt() != 8))
			context.throwUnexpectedElementError("element width 1, 2, 4, or 8", width);
	}

	private static void verifyConstant(ProcessorContext ctx, ASTElement element) {
		switch (element.type()) {
			case NUMBER, STRING, CHARACTER, BOOL, ENUM -> {}
			case DECLARATION -> verifyDeclarationConstant(ctx, element);
			case IDENTIFIER -> verifyConstantIdentifier(ctx, element);
			case ARRAY -> // only handle
					HandleOperands.verify(ctx, element);
		}
	}

	/**
	 * Validates an identifier constant against the shapes the Dalvik constant mapper can resolve.
	 *
	 * @param ctx
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Identifier constant to validate.
	 */
	private static void verifyConstantIdentifier(ProcessorContext ctx, ASTElement element) {
		String content = element.content();

		// Handle shortcuts are resolved by name and never parsed as a type. One of them starts with
		// 'L', so this check has to come before the descriptor forms below.
		if (Handle.HANDLE_SHORTCUTS.containsKey(content))
			return;

		switch (content.charAt(0)) {
			case '(' ->
					ctx.validateDescriptor(element, DescriptorForm.METHOD, "class, method or array descriptor", element);
			case '[' ->
					ctx.validateDescriptor(element, DescriptorForm.FIELD, "class, method or array descriptor", element);
			case 'L' -> {
				if (content.endsWith(";")) {
					ctx.validateDescriptor(element, DescriptorForm.FIELD, "class or array descriptor", element);
				} else if (content.contains("/")) {
					// An 'L'-prefixed name with a package path and no terminator is an internal name.
					ctx.validateDescriptor(element, DescriptorForm.INTERNAL_NAME, "class or array descriptor", element);
				} else {
					// Neither a descriptor nor a name with a path the mapper could resolve.
					ctx.throwUnexpectedElementError("class, method or array descriptor", element);
				}
			}
			// Bare names cover the mapper's named values (null, booleans, special numbers) and plain
			// internal names, both of which are legal as identifiers.
			default ->
					ctx.validateDescriptor(element, DescriptorForm.INTERNAL_NAME, "class, method or array descriptor", element);
		}
	}

	/**
	 * Validates a declaration constant against the shapes the Dalvik constant mapper can resolve.
	 *
	 * @param ctx
	 * 		Processor context receiving diagnostics.
	 * @param element
	 * 		Declaration constant to validate.
	 */
	private static void verifyDeclarationConstant(ProcessorContext ctx, ASTElement element) {
		if (!(element instanceof ASTDeclaration declaration) || declaration.keyword() == null) {
			ctx.throwUnexpectedElementError("enum or member constant", element);
			return;
		}

		String keyword = declaration.keyword().content();
		int size = declaration.elements().size();
		boolean validEnum = ".enum".equals(keyword) && (size == 2 || size == 3);
		boolean validMember = ".member".equals(keyword) && size == 3;
		if (!validEnum && !validMember) {
			ctx.throwUnexpectedElementError("enum or member constant", element);
			return;
		}

		for (ASTElement value : declaration.elements())
			if (contextIsNotIdentifier(ctx, value, element))
				return;
	}

	private static boolean contextIsNotIdentifier(ProcessorContext ctx, ASTElement value, ASTElement parent) {
		if (value == null || value.type() != ElementType.IDENTIFIER) {
			ctx.throwUnexpectedElementError("constant identifier", value == null ? parent : value);
			return true;
		}
		return false;
	}

	/**
	 * @return Declared shape of a packed switch payload: the lowest key and the target labels.
	 */
	private static PayloadSchema packedSwitchSchema() {
		return PayloadSchema.fixed("packed switch",
				PayloadSchema.Field.value("first", ElementType.NUMBER, "number"),
				PayloadSchema.Field.arrayOf("targets", "targets", ElementType.IDENTIFIER, "label"));
	}

	/**
	 * @return Declared shape of a sparse switch payload: integer case keys with label values.
	 */
	private static PayloadSchema sparseSwitchSchema() {
		return PayloadSchema.keyed("sparse switch",
				new PayloadSchema.CaseKeys(ElementType.IDENTIFIER, "identifier"));
	}

	/**
	 * @return Declared shape of the object form of an array-data payload: a width and the literals.
	 */
	private static PayloadSchema dataPayloadSchema() {
		return PayloadSchema.fixed("data payload",
				PayloadSchema.Field.value("width", ElementType.NUMBER, "number"),
				PayloadSchema.Field.arrayOf("values", "data payload values", ElementType.NUMBER, "numeric literal"));
	}
}
