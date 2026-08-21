package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;

/**
 * Registry for {@link System} static methods.
 */
public final class SystemMethodValueRegistry {
    private SystemMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        // Environment-sensitive: line separator depends on the host runtime.
        builder.registerStatic("java/lang/System", "lineSeparator", "()Ljava/lang/String;",
                params -> Values.valueOfString(System.lineSeparator()));
        // Environment-sensitive: this depends on the host JVM's system properties.
        builder.registerStatic("java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue key) {
                try {
                    String property = System.getProperty(key.value());
                    if (property != null)
                        return Values.valueOfString(property);
                } catch (Throwable ignored) {
                }
            }
            return Values.STRING_VALUE;
        });
        // Environment-sensitive: this depends on the host JVM's system properties.
        builder.registerStatic("java/lang/System", "getProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue key &&
                    params.get(1) instanceof Value.KnownStringValue fallback) {
                try {
                    String property = System.getProperty(key.value(), fallback.value());
                    if (property != null)
                        return Values.valueOfString(property);
                } catch (Throwable ignored) {
                }
            }
            return Values.STRING_VALUE;
        });
        return builder.build();
    }
}
