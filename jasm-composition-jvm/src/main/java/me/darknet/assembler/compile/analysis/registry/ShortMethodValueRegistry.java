package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;

/**
 * Registry for {@link Short} static methods.
 */
public final class ShortMethodValueRegistry {
    private ShortMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Short", "parseShort", "(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Short.parseShort(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "parseShort", "(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Short.parseShort(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "toString", "(S)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue value)
                return Values.valueOfString(Short.toString((short) value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Short", "toUnsignedInt", "(S)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.toUnsignedInt((short) value.value()));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "toUnsignedLong", "(S)J", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.toUnsignedLong((short) value.value()));
            return Values.LONG_VALUE;
        });
        builder.registerStatic("java/lang/Short", "reverseBytes", "(S)S", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.reverseBytes((short) value.value()));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "hashCode", "(S)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.hashCode((short) value.value()));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "compare", "(SS)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Short.compare((short) a.value(), (short) b.value()));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "compareUnsigned", "(SS)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Short.compareUnsigned((short) a.value(), (short) b.value()));
            return Values.INT_VALUE;
        });
        return builder.build();
    }
}
