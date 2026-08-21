package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.func.J2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.J2JStaticFunc;
import me.darknet.assembler.compile.analysis.func.JI2JThrowingStaticFunc;
import me.darknet.assembler.compile.analysis.func.JJ2IThrowingStaticFunc;
import me.darknet.assembler.compile.analysis.func.JJ2JThrowingStaticFunc;

/**
 * Registry for {@link Long} static methods.
 */
public final class LongMethodValueRegistry {
    private LongMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Long", "parseLong", "(Ljava/lang/String;)J", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Long.parseLong(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.LONG_VALUE;
        });
        builder.registerStatic("java/lang/Long", "parseLong", "(Ljava/lang/String;I)J", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Long.parseLong(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.LONG_VALUE;
        });
        builder.registerStatic("java/lang/Long", "parseLong", "(Ljava/lang/CharSequence;III)J", params -> {
            if (params.size() == 4 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue begin &&
                    params.get(2) instanceof Value.KnownIntValue end &&
                    params.get(3) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Long.parseLong(text.value(), begin.value(), end.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.LONG_VALUE;
        });
        builder.registerStatic("java/lang/Long", "toString", "(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Long", "toString", "(JI)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownLongValue value &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                return Values.valueOfString(Long.toString(value.value(), radix.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Long", "toUnsignedString", "(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toUnsignedString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Long", "toUnsignedString", "(JI)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownLongValue value &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                return Values.valueOfString(Long.toUnsignedString(value.value(), radix.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Long", "toHexString", "(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toHexString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Long", "toOctalString", "(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toOctalString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Long", "hashCode", "(J)I", (J2IStaticFunc) Long::hashCode);
        builder.registerStatic("java/lang/Long", "signum", "(J)I", (J2IStaticFunc) Long::signum);
        builder.registerStatic("java/lang/Long", "reverse", "(J)J", (J2JStaticFunc) Long::reverse);
        builder.registerStatic("java/lang/Long", "compare", "(JJ)I", (JJ2IThrowingStaticFunc) Long::compare);
        builder.registerStatic("java/lang/Long", "compareUnsigned", "(JJ)I", (JJ2IThrowingStaticFunc) Long::compareUnsigned);
        builder.registerStatic("java/lang/Long", "divideUnsigned", "(JJ)J", (JJ2JThrowingStaticFunc) Long::divideUnsigned);
        builder.registerStatic("java/lang/Long", "remainderUnsigned", "(JJ)J", (JJ2JThrowingStaticFunc) Long::remainderUnsigned);
        builder.registerStatic("java/lang/Long", "highestOneBit", "(J)J", (J2JStaticFunc) Long::highestOneBit);
        builder.registerStatic("java/lang/Long", "lowestOneBit", "(J)J", (J2JStaticFunc) Long::lowestOneBit);
        builder.registerStatic("java/lang/Long", "numberOfLeadingZeros", "(J)I", (J2IStaticFunc) Long::numberOfLeadingZeros);
        builder.registerStatic("java/lang/Long", "numberOfTrailingZeros", "(J)I", (J2IStaticFunc) Long::numberOfTrailingZeros);
        builder.registerStatic("java/lang/Long", "bitCount", "(J)I", (J2IStaticFunc) Long::bitCount);
        builder.registerStatic("java/lang/Long", "rotateLeft", "(JI)J", (JI2JThrowingStaticFunc) Long::rotateLeft);
        builder.registerStatic("java/lang/Long", "rotateRight", "(JI)J", (JI2JThrowingStaticFunc) Long::rotateRight);
        return builder.build();
    }
}
