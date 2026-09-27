package me.darknet.assembler.backend.jvm.printer;

import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.PrintContext.FloatPrintMode;
import me.darknet.assembler.util.EscapeUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Prints JVM constants, handles, type literals, and dynamic constants into source output.
 */
record JvmConstantPrinter(PrintContext<?> ctx) {
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
	private static final Map<Integer, String> HANDLE_TYPES = Map.of(
			Opcodes.H_GETFIELD, "getfield",
			Opcodes.H_GETSTATIC, "getstatic",
			Opcodes.H_PUTFIELD, "putfield",
			Opcodes.H_PUTSTATIC, "putstatic",
			Opcodes.H_INVOKEVIRTUAL, "invokevirtual",
			Opcodes.H_INVOKESTATIC, "invokestatic",
			Opcodes.H_INVOKESPECIAL, "invokespecial",
			Opcodes.H_NEWINVOKESPECIAL, "newinvokespecial",
			Opcodes.H_INVOKEINTERFACE, "invokeinterface"
	);

	static {
		DECIMAL_FORMAT.setMinimumIntegerDigits(1);
		DECIMAL_FORMAT.setMaximumFractionDigits(10);
	}

	/**
	 * Prints a method handle in a human-readable format, including its kind, owner, name, and descriptor.
	 * If a shortcut representation exists for the handle, it will be used instead.
	 *
	 * @param handle
	 * 		The method handle to print.
	 * @param ctx
	 * 		The print context to use for printing.
	 */
	public static void printMethodHandle(org.objectweb.asm.Handle handle, PrintContext<?> ctx) {
		String owner = handle.getOwner();
		String name = handle.getName();
		String descriptor = handle.getDesc();
		String shortHandle = Handle.SHORTCUT_LOOKUP.get(owner + "." + name + descriptor);
		if (shortHandle != null) {
			ctx.append(shortHandle);
			return;
		}
		var array = ctx.array();
		String kind = HANDLE_TYPES.get(handle.getTag());
		array.print(kind).arg().literal(owner).append(".").literal(name).arg().literal(descriptor).end();
	}

	/**
	 * Prints a type literal, which can be either an object type or a primitive type.
	 * <ul>
	 *     <li>For object types, the internal name is printed ("java/lang/String").</li>
	 *     <li>For primitive types, the descriptor is printed ("I" for int).</li>
	 * </ul>
	 *
	 * @param type
	 * 		The type to print.
	 * @param ctx
	 * 		The print context to use for printing.
	 */
	public static void printTypeLiteral(Type type, PrintContext<?> ctx) {
		if (type.getSort() == Type.OBJECT) {
			ctx.literal(type.getInternalName());
		} else {
			ctx.literal(type.getDescriptor());
		}
	}

	/**
	 * Prints a constant value, which can be of various types including:
	 * <ul>
	 *     <li>null</li>
	 *     <li>String</li>
	 *     <li>Integer</li>
	 *     <li>Long</li>
	 *     <li>Float</li>
	 *     <li>Double</li>
	 *     <li>Byte</li>
	 *     <li>Short</li>
	 *     <li>Boolean</li>
	 *     <li>Character</li>
	 *     <li>Type</li>
	 *     <li>Handle</li>
	 *     <li>ConstantDynamic</li>
	 * </ul>
	 *
	 * @param value
	 * 		The constant value to print.
	 */
	public void printConstant(Object value) {
		switch (value) {
			case null -> ctx.print("null");
			case String stringValue -> ctx.string(stringValue);
			case Integer intValue -> ctx.print(String.valueOf(intValue));
			case Long longValue -> ctx.print(String.valueOf(longValue)).print("L");
			case Float floatValue -> printFloat(floatValue, ctx);
			case Double doubleValue -> printDouble(doubleValue, ctx);
			case Byte byteValue -> ctx.print(String.valueOf(byteValue));
			case Short shortValue -> ctx.print(String.valueOf(shortValue));
			case Boolean booleanValue -> ctx.print(String.valueOf(booleanValue));
			case Character charValue ->
					ctx.print("'").print(EscapeUtil.escapeString(String.valueOf(charValue))).print("'");
			case Type typeValue -> ctx.literal(typeValue.getDescriptor());
			case org.objectweb.asm.Handle handleValue -> printMethodHandle(handleValue, ctx);
			case ConstantDynamic dynamicValue -> printDynamic(dynamicValue);
			default -> throw new IllegalStateException("Unexpected constant value: " + value);
		}
	}

	/**
	 * Prints a {@link ConstantDynamic} value, including its name, descriptor, bootstrap method, and bootstrap method arguments.
	 *
	 * @param dynamic
	 * 		The {@link ConstantDynamic} value to print.
	 */
	private void printDynamic(ConstantDynamic dynamic) {
		var array = ctx.array();
		array.literal(dynamic.getName()).arg().literal(dynamic.getDescriptor()).arg();
		printMethodHandle(dynamic.getBootstrapMethod(), ctx);
		var bsmArray = array.arg().array();
		for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) {
			if (i > 0) {
				bsmArray.arg();
			}
			new JvmConstantPrinter(bsmArray).printConstant(dynamic.getBootstrapMethodArgument(i));
		}
		bsmArray.end();
		array.end();
	}

	/**
	 * Prints a double value according to the configured float print mode.
	 * <ul>
	 *     <li>In {@link FloatPrintMode#STANDARD} mode, the value is printed as a decimal number, with an optional "D" suffix.</li>
	 *     <li>In {@link FloatPrintMode#HEX} mode, the raw bits of the double are printed in hexadecimal format.</li>
	 *     <li>In {@link FloatPrintMode#BINARY} mode, the raw bits of the double are printed in binary format.</li>
	 * </ul>
	 *
	 * @param value
	 * 		The double value to print.
	 * @param ctx
	 * 		The print context to use for printing.
	 */
	public static void printDouble(double value, PrintContext<?> ctx) {
		switch (ctx.floatPrintMode()) {
			// Decimal NaN text loses the payload, so raw bits are an explicit lossless opt-in.
			case HEX -> ctx.print(rawBits(Double.doubleToRawLongBits(value), 16, 16));
			case BINARY -> ctx.print(rawBits(Double.doubleToRawLongBits(value), 64, 2));
			case STANDARD -> {
				String content = ctx.forceWholeNumberRepresentation() && Double.isFinite(value)
						? DECIMAL_FORMAT.format(value)
						: String.valueOf(value);
				ctx.print(content);
				if (!content.matches("\\D+")) {
					ctx.print("D");
				}
			}
		}
	}

	/**
	 * Prints a float value according to the configured float print mode.
	 * <ul>
	 *     <li>In {@link FloatPrintMode#STANDARD} mode, the value is printed as a decimal number, with an optional "F" suffix.</li>
	 *     <li>In {@link FloatPrintMode#HEX} mode, the raw bits of the float are printed in hexadecimal format.</li>
	 *     <li>In {@link FloatPrintMode#BINARY} mode, the raw bits of the float are printed in binary format.</li>
	 * </ul>
	 *
	 * @param value
	 * 		The float value to print.
	 * @param ctx
	 * 		The print context to use for printing.
	 */
	public static void printFloat(float value, PrintContext<?> ctx) {
		switch (ctx.floatPrintMode()) {
			// Decimal NaN text loses the payload, so raw bits are an explicit lossless opt-in.
			case HEX -> ctx.print(rawBits(Integer.toUnsignedLong(Float.floatToRawIntBits(value)), 8, 16));
			case BINARY -> ctx.print(rawBits(Integer.toUnsignedLong(Float.floatToRawIntBits(value)), 32, 2));
			case STANDARD -> {
				String content = ctx.forceWholeNumberRepresentation() && Float.isFinite(value)
						? DECIMAL_FORMAT.format(value)
						: String.valueOf(value);
				ctx.print(content).print("F");
			}
		}
	}

	/**
	 * @param bits
	 * 		The raw bits of the floating-point value.
	 * @param width
	 * 		The width of the output string (in characters).
	 * @param radix
	 * 		The radix to use for the output string (2 for binary, 16 for hexadecimal).
	 *
	 * @return A string representation of the raw bits in the specified radix, padded with leading zeros to the specified width.
	 */
	private static String rawBits(long bits, int width, int radix) {
		String digits = Long.toUnsignedString(bits, radix);
		if (radix == 16)
			digits = digits.toUpperCase();
		return "#0" + (radix == 16 ? "x" : "b") + "0".repeat(width - digits.length()) + digits;
	}
}
