package me.darknet.assembler.printer;

import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.util.EscapeUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.text.DecimalFormat;
import java.util.Map;

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

    public static void printTypeLiteral(Type type, PrintContext<?> ctx) {
        if (type.getSort() == Type.OBJECT) {
            ctx.literal(type.getInternalName());
        } else {
            ctx.literal(type.getDescriptor());
        }
    }

    public void printConstant(Object value) {
        switch (value) {
            case null -> ctx.print("null");
            case String stringValue -> ctx.string(stringValue);
            case Integer intValue -> ctx.print(String.valueOf(intValue));
            case Long longValue -> ctx.print(String.valueOf(longValue)).print("L");
            case Float floatValue -> printFloat(floatValue);
            case Double doubleValue -> printDouble(doubleValue);
            case Byte byteValue -> ctx.print(String.valueOf(byteValue));
            case Short shortValue -> ctx.print(String.valueOf(shortValue));
            case Boolean booleanValue -> ctx.print(String.valueOf(booleanValue));
            case Character charValue -> ctx.print("'").print(EscapeUtil.escapeString(String.valueOf(charValue))).print("'");
            case Type typeValue -> ctx.literal(typeValue.getDescriptor());
            case org.objectweb.asm.Handle handleValue -> printMethodHandle(handleValue, ctx);
            case ConstantDynamic dynamicValue -> printDynamic(dynamicValue);
            default -> throw new IllegalStateException("Unexpected constant value: " + value);
        }
    }

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

    private void printDouble(double value) {
        String content = ctx.forceWholeNumberRepresentation && Double.isFinite(value)
                ? DECIMAL_FORMAT.format(value)
                : String.valueOf(value);
        ctx.print(content);
        if (!content.matches("\\D+")) {
            ctx.print("D");
        }
    }

    private void printFloat(float value) {
        String content = ctx.forceWholeNumberRepresentation && Float.isFinite(value)
                ? DECIMAL_FORMAT.format(value)
                : String.valueOf(value);
        ctx.print(content).print("F");
    }
}
