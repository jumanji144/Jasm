package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.func.F2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.F2ZStaticFunc;
import me.darknet.assembler.compile.analysis.func.FF2FStaticFunc;
import me.darknet.assembler.compile.analysis.func.FF2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.I2FStaticFunc;

/**
 * Registry for {@link Float} static methods.
 */
public final class FloatMethodValueRegistry {
    private FloatMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Float.parseFloat(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.FLOAT_VALUE;
        });
        builder.registerStatic("java/lang/Float", "toHexString", "(F)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownFloatValue value)
                return Values.valueOfString(Float.toHexString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Float", "toString", "(F)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownFloatValue value)
                return Values.valueOfString(Float.toString(value.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Float", "isNaN", "(F)Z", (F2ZStaticFunc) Float::isNaN);
        builder.registerStatic("java/lang/Float", "isFinite", "(F)Z", (F2ZStaticFunc) Float::isFinite);
        builder.registerStatic("java/lang/Float", "isInfinite", "(F)Z", (F2ZStaticFunc) Float::isInfinite);
        builder.registerStatic("java/lang/Float", "floatToRawIntBits", "(F)I", (F2IStaticFunc) Float::floatToRawIntBits);
        builder.registerStatic("java/lang/Float", "floatToIntBits", "(F)I", (F2IStaticFunc) Float::floatToIntBits);
        builder.registerStatic("java/lang/Float", "intBitsToFloat", "(I)F", (I2FStaticFunc) Float::intBitsToFloat);
        builder.registerStatic("java/lang/Float", "hashCode", "(F)I", (F2IStaticFunc) Float::hashCode);
        builder.registerStatic("java/lang/Float", "compare", "(FF)I", (FF2IStaticFunc) Float::compare);
        builder.registerStatic("java/lang/Float", "min", "(FF)F", (FF2FStaticFunc) Float::min);
        builder.registerStatic("java/lang/Float", "max", "(FF)F", (FF2FStaticFunc) Float::max);
        builder.registerStatic("java/lang/Float", "sum", "(FF)F", (FF2FStaticFunc) Float::sum);
        return builder.build();
    }
}
