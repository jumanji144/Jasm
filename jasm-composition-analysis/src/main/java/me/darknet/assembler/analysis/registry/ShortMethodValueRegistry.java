package me.darknet.assembler.analysis.registry;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;

/**
 * Registry for {@link Short} static methods.
 */
public final class ShortMethodValueRegistry {
    private ShortMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Short", "parseShort", "(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String text))
                try {
                    return Values.valueOf(Short.parseShort(text));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "parseShort", "(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownStringValue(String text) &&
		            params.get(1) instanceof Value.KnownIntValue(int radix))
                try {
                    return Values.valueOf(Short.parseShort(text, radix));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "toString", "(S)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int value))
                return Values.valueOfString(Short.toString((short) value));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Short", "toUnsignedInt", "(S)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int value))
                return Values.valueOf(Short.toUnsignedInt((short) value));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "toUnsignedLong", "(S)J", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int value))
                return Values.valueOf(Short.toUnsignedLong((short) value));
            return Values.LONG_VALUE;
        });
        builder.registerStatic("java/lang/Short", "reverseBytes", "(S)S", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int value))
                return Values.valueOf(Short.reverseBytes((short) value));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "hashCode", "(S)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int value))
                return Values.valueOf(Short.hashCode((short) value));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "compare", "(SS)I", params -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int a) &&
		            params.get(1) instanceof Value.KnownIntValue(int b))
                return Values.valueOf(Short.compare((short) a, (short) b));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Short", "compareUnsigned", "(SS)I", params -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int a) &&
		            params.get(1) instanceof Value.KnownIntValue(int b))
                return Values.valueOf(Short.compareUnsigned((short) a, (short) b));
            return Values.INT_VALUE;
        });
        return builder.build();
    }
}
