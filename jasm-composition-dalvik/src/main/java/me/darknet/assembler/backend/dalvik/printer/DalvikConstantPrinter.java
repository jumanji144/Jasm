package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.backend.dalvik.DalvikHandleKinds;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.util.EscapeUtil;
import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.BoolConstant;
import me.darknet.dex.tree.definitions.constant.ByteConstant;
import me.darknet.dex.tree.definitions.constant.CharConstant;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.DoubleConstant;
import me.darknet.dex.tree.definitions.constant.EnumConstant;
import me.darknet.dex.tree.definitions.constant.FloatConstant;
import me.darknet.dex.tree.definitions.constant.Handle;
import me.darknet.dex.tree.definitions.constant.HandleConstant;
import me.darknet.dex.tree.definitions.constant.IntConstant;
import me.darknet.dex.tree.definitions.constant.LongConstant;
import me.darknet.dex.tree.definitions.constant.MemberConstant;
import me.darknet.dex.tree.definitions.constant.MethodTypeConstant;
import me.darknet.dex.tree.definitions.constant.NullConstant;
import me.darknet.dex.tree.definitions.constant.ShortConstant;
import me.darknet.dex.tree.definitions.constant.StringConstant;
import me.darknet.dex.tree.definitions.constant.TypeConstant;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.Type;

import java.util.List;
import java.util.Locale;

import static me.darknet.assembler.backend.dalvik.printer.DalvikAnnotationPrinter.VISIBILITY_INTERNAL;

/**
 * Utility class for printing constants in the Dalvik assembly format.
 */
public class DalvikConstantPrinter {
	/**
	 * Prints a method handle in the Dalvik assembly format.
	 *
	 * @param handle
	 * 		The method handle to print.
	 * @param ctx
	 * 		The print context to use for output.
	 */
	public static void printHandle(Handle handle, PrintContext<?> ctx) {
		String owner = handle.owner().internalName();
		String name = handle.name();
		String descriptor = handle.type().descriptor();
		String shortHandle = me.darknet.assembler.helper.Handle.SHORTCUT_LOOKUP.get(owner + "." + name + descriptor);
		if (shortHandle != null) {
			// We are intentionally using append because our 'short handle' is safe and does not need to be escaped
			ctx.append(shortHandle);
			return;
		}
		var array = ctx.array();
		String kind = DalvikHandleKinds.keyword(handle.kind());
		if (kind == null)
			throw new IllegalStateException("Unsupported method-handle kind: 0x" + Integer.toHexString(handle.kind()));
		array.print(kind).arg().literal(owner).append(".").literal(name).arg().literal(descriptor).end();
	}

	/**
	 * Prints an annotation in the Dalvik assembly format.
	 *
	 * @param ctx
	 * 		The print context to use for output.
	 * @param part
	 * 		The annotation part to print.
	 */
	private static void printAnnotation(PrintContext<?> ctx, AnnotationPart part) {
		var printer = new DalvikAnnotationPrinter(new Annotation(VISIBILITY_INTERNAL, part));
		printer.print(ctx);
	}

	/**
	 * Prints a constant in the Dalvik assembly format.
	 *
	 * @param ctx
	 * 		The print context to use for output.
	 * @param constant
	 * 		The constant to print.
	 */
	public static void printConstant(PrintContext<?> ctx, Constant constant) {
		switch (constant) {
			case AnnotationConstant(AnnotationPart part) -> printAnnotation(ctx, part);
			case ArrayConstant(List<Constant> constants) -> {
				var array = ctx.array();
				array.print(constants, DalvikConstantPrinter::printConstant);
				array.end();
			}
			case BoolConstant(boolean value) -> ctx.print(Boolean.toString(value));
			case ByteConstant(byte value) -> ctx.print(Byte.toString(value));
			case CharConstant(char value) -> {
				String str = String.valueOf(value);
				ctx.print("'").print(EscapeUtil.escapeString(str)).print("'");
			}
			case EnumConstant(InstanceType owner, MemberIdentifier field) -> ctx.element(".enum")
					.literal(owner.internalName()).print(" ")
					.literal(field.name()).print(" ")
					.literal(field.descriptor());
			case FloatConstant(float value) -> printFloat(ctx, value);
			case DoubleConstant(double value) -> printDouble(ctx, value);
			case IntConstant(int value) -> ctx.print(Integer.toString(value));
			case LongConstant(long value) -> ctx.print(value + "L");
			case ShortConstant(short value) -> ctx.print(Short.toString(value));
			case NullConstant ignored -> ctx.print("null");
			case StringConstant(String value) -> ctx.string(value);
			case TypeConstant(Type type) -> ctx.literal(
					type instanceof InstanceType instanceType ? instanceType.internalName() : type.descriptor()
			);
			case MethodTypeConstant(MethodType type) -> ctx.literal(type.descriptor());
			case HandleConstant(Handle handle) -> printHandle(handle, ctx);
			case MemberConstant(InstanceType owner, MemberIdentifier member) -> ctx.element(".member")
					.literal(owner.internalName())
					.print(" ")
					.literal(member.name())
					.print(" ")
					.literal(member.descriptor());

			default -> throw new IllegalStateException("Unexpected value: " + constant);
		}
	}

	/**
	 * Prints a float value in the Dalvik assembly format.
	 *
	 * @param ctx
	 * 		The print context to use for output.
	 * @param value
	 * 		The float value to print.
	 */
	public static void printFloat(PrintContext<?> ctx, float value) {
		switch (ctx.floatPrintMode()) {
			case HEX -> ctx.print(rawBits(Integer.toUnsignedLong(Float.floatToRawIntBits(value)), 8, 16));
			case BINARY -> ctx.print(rawBits(Integer.toUnsignedLong(Float.floatToRawIntBits(value)), 32, 2));
			case STANDARD -> ctx.print(value + "F");
		}
	}

	/**
	 * Prints a double value in the Dalvik assembly format.
	 *
	 * @param ctx
	 * 		The print context to use for output.
	 * @param value
	 * 		The double value to print.
	 */
	public static void printDouble(PrintContext<?> ctx, double value) {
		switch (ctx.floatPrintMode()) {
			case HEX -> ctx.print(rawBits(Double.doubleToRawLongBits(value), 16, 16));
			case BINARY -> ctx.print(rawBits(Double.doubleToRawLongBits(value), 64, 2));
			case STANDARD -> {
				String content = Double.toString(value);
				ctx.print(content);
				if (!content.matches("\\D+"))
					ctx.print("D");
			}
		}
	}

	/**
	 * Converts raw bits to a string representation in the specified radix with padding.
	 *
	 * @param bits
	 * 		The raw bits to convert.
	 * @param width
	 * 		The width of the output string (number of digits).
	 * @param radix
	 * 		The radix/base for conversion (e.g., 2 for binary, 16 for hexadecimal).
	 *
	 * @return A string representation of the raw bits in the specified radix with padding.
	 */
	private static String rawBits(long bits, int width, int radix) {
		String digits = Long.toUnsignedString(bits, radix);
		if (radix == 16)
			digits = digits.toUpperCase();
		return "#0" + (radix == 16 ? "x" : "b") + "0".repeat(width - digits.length()) + digits;
	}
}
