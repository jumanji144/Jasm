package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;

/**
 * Registry for {@link Boolean} static methods.
 */
public final class BooleanMethodValueRegistry {
    private BooleanMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStatic("java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue value)
                return Values.valueOf(Boolean.parseBoolean(value.value()));
            return Values.INT_VALUE;
        });
        // Environment-sensitive: this depends on the host JVM's system properties.
        builder.registerStatic("java/lang/Boolean", "getBoolean", "(Ljava/lang/String;)Z", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue value)
                return Values.valueOf(Boolean.getBoolean(value.value()));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Boolean", "logicalAnd", "(ZZ)Z", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.logicalAnd(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Boolean", "logicalOr", "(ZZ)Z", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.logicalOr(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Boolean", "logicalXor", "(ZZ)Z", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.logicalXor(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Boolean", "compare", "(ZZ)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.compare(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Boolean", "toString", "(Z)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue value)
                return Values.valueOfString(Boolean.toString(value.value() != 0));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/Boolean", "hashCode", "(Z)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue value)
                return Values.valueOf(Boolean.hashCode(value.value() != 0));
            return Values.INT_VALUE;
        });
        return builder.build();
    }
}
