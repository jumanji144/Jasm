package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.func.D2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.D2JStaticFunc;
import me.darknet.assembler.compile.analysis.func.D2ZStaticFunc;
import me.darknet.assembler.compile.analysis.func.DD2DStaticFunc;
import me.darknet.assembler.compile.analysis.func.DD2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.J2DStaticFunc;

/**
 * Registry for {@link Double} static methods.
 */
public final class DoubleMethodValueRegistry {
    private DoubleMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Double", "parseDouble", "(Ljava/lang/String;)D", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Double.parseDouble(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.DOUBLE_VALUE;
        });
        builder.registerStatic("java/lang/Double", "toString", "(D)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownDoubleValue value)
                return Values.valueOfString(Double.toString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Double", "toHexString", "(D)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownDoubleValue value)
                return Values.valueOfString(Double.toHexString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Double", "isNaN", "(D)Z", (D2ZStaticFunc) Double::isNaN);
        builder.registerStatic("java/lang/Double", "isFinite", "(D)Z", (D2ZStaticFunc) Double::isFinite);
        builder.registerStatic("java/lang/Double", "isInfinite", "(D)Z", (D2ZStaticFunc) Double::isInfinite);
        builder.registerStatic("java/lang/Double", "hashCode", "(D)I", (D2IStaticFunc) Double::hashCode);
        builder.registerStatic("java/lang/Double", "doubleToLongBits", "(D)J", (D2JStaticFunc) Double::doubleToLongBits);
        builder.registerStatic("java/lang/Double", "doubleToRawLongBits", "(D)J", (D2JStaticFunc) Double::doubleToRawLongBits);
        builder.registerStatic("java/lang/Double", "longBitsToDouble", "(J)D", (J2DStaticFunc) Double::longBitsToDouble);
        builder.registerStatic("java/lang/Double", "min", "(DD)D", (DD2DStaticFunc) Double::min);
        builder.registerStatic("java/lang/Double", "max", "(DD)D", (DD2DStaticFunc) Double::max);
        builder.registerStatic("java/lang/Double", "sum", "(DD)D", (DD2DStaticFunc) Double::sum);
        builder.registerStatic("java/lang/Double", "compare", "(DD)I", (DD2IStaticFunc) Double::compare);
        return builder.build();
    }
}
