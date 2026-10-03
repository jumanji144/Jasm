package me.darknet.assembler.analysis.registry;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.analysis.func.I2IStaticFunc;
import me.darknet.assembler.analysis.func.I2JStaticFunc;
import me.darknet.assembler.analysis.func.I2StringStaticFunc;
import me.darknet.assembler.analysis.func.II2IStaticFunc;

/**
 * Registry for {@link Integer} static methods.
 */
public final class IntegerMethodValueRegistry {
    private IntegerMethodValueRegistry() {}

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String text))
                try {
                    return Values.valueOf(Integer.parseInt(text));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Integer", "parseInt", "(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownStringValue(String text) &&
		            params.get(1) instanceof Value.KnownIntValue(int radix))
                try {
                    return Values.valueOf(Integer.parseInt(text, radix));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Integer", "parseUnsignedInt", "(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String text))
                try {
                    return Values.valueOf(Integer.parseUnsignedInt(text));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Integer", "parseUnsignedInt", "(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownStringValue(String text) &&
		            params.get(1) instanceof Value.KnownIntValue(int radix))
                try {
                    return Values.valueOf(Integer.parseUnsignedInt(text, radix));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Integer", "signum", "(I)I", (I2IStaticFunc) Integer::signum);
        builder.registerStatic("java/lang/Integer", "hashCode", "(I)I", (I2IStaticFunc) Integer::hashCode);
        builder.registerStatic("java/lang/Integer", "reverse", "(I)I", (I2IStaticFunc) Integer::reverse);
        builder.registerStatic("java/lang/Integer", "reverseBytes", "(I)I", (I2IStaticFunc) Integer::reverseBytes);
        builder.registerStatic("java/lang/Integer", "lowestOneBit", "(I)I", (I2IStaticFunc) Integer::lowestOneBit);
        builder.registerStatic("java/lang/Integer", "highestOneBit", "(I)I", (I2IStaticFunc) Integer::highestOneBit);
        builder.registerStatic("java/lang/Integer", "numberOfLeadingZeros", "(I)I", (I2IStaticFunc) Integer::numberOfLeadingZeros);
        builder.registerStatic("java/lang/Integer", "numberOfTrailingZeros", "(I)I", (I2IStaticFunc) Integer::numberOfTrailingZeros);
        builder.registerStatic("java/lang/Integer", "bitCount", "(I)I", (I2IStaticFunc) Integer::bitCount);
        builder.registerStatic("java/lang/Integer", "compare", "(II)I", (II2IStaticFunc) Integer::compare);
        builder.registerStatic("java/lang/Integer", "compareUnsigned", "(II)I", (II2IStaticFunc) Integer::compareUnsigned);
        builder.registerStatic("java/lang/Integer", "divideUnsigned", "(II)I", (II2IStaticFunc) Integer::divideUnsigned);
        builder.registerStatic("java/lang/Integer", "remainderUnsigned", "(II)I", (II2IStaticFunc) Integer::remainderUnsigned);
        builder.registerStatic("java/lang/Integer", "min", "(II)I", (II2IStaticFunc) Integer::min);
        builder.registerStatic("java/lang/Integer", "max", "(II)I", (II2IStaticFunc) Integer::max);
        builder.registerStatic("java/lang/Integer", "rotateLeft", "(II)I", (II2IStaticFunc) Integer::rotateLeft);
        builder.registerStatic("java/lang/Integer", "rotateRight", "(II)I", (II2IStaticFunc) Integer::rotateRight);
        builder.registerStatic("java/lang/Integer", "toUnsignedLong", "(I)J", (I2JStaticFunc) Integer::toUnsignedLong);
        builder.registerStatic("java/lang/Integer", "toBinaryString", "(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toBinaryString);
        builder.registerStatic("java/lang/Integer", "toHexString", "(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toHexString);
        builder.registerStatic("java/lang/Integer", "toOctalString", "(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toOctalString);
        builder.registerStatic("java/lang/Integer", "toUnsignedString", "(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toUnsignedString);
        builder.registerStatic("java/lang/Integer", "toUnsignedString", "(II)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int a) &&
		            params.get(1) instanceof Value.KnownIntValue(int b))
                return Values.valueOfString(Integer.toUnsignedString(a, b));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toString);
        builder.registerStatic("java/lang/Integer", "toString", "(II)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int a) &&
		            params.get(1) instanceof Value.KnownIntValue(int b))
                return Values.valueOfString(Integer.toString(a, b));
            return Values.STRING_VALUE;
        });
        return builder.build();
    }
}
