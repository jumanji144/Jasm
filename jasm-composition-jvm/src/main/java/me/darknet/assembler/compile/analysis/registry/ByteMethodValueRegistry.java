package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.func.B2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.B2JStaticFunc;
import me.darknet.assembler.compile.analysis.func.B2StringStaticFunc;
import me.darknet.assembler.compile.analysis.func.BB2IStaticFunc;

/**
 * Registry for {@link Byte} static methods.
 */
public final class ByteMethodValueRegistry {
    private ByteMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Byte", "parseByte", "(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Byte.parseByte(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Byte", "parseByte", "(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Byte.parseByte(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Byte", "decode", "(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Byte.decode(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Byte", "compare", "(BB)I", (BB2IStaticFunc) Byte::compare);
        builder.registerStatic("java/lang/Byte", "compareUnsigned", "(BB)I", (BB2IStaticFunc) Byte::compareUnsigned);
        builder.registerStatic("java/lang/Byte", "toString", "(B)Ljava/lang/String;", (B2StringStaticFunc) Byte::toString);
        builder.registerStatic("java/lang/Byte", "hashCode", "(B)I", (B2IStaticFunc) Byte::hashCode);
        builder.registerStatic("java/lang/Byte", "toUnsignedInt", "(B)I", (B2IStaticFunc) Byte::toUnsignedInt);
        builder.registerStatic("java/lang/Byte", "toUnsignedLong", "(B)J", (B2JStaticFunc) Byte::toUnsignedLong);
        return builder.build();
    }
}
