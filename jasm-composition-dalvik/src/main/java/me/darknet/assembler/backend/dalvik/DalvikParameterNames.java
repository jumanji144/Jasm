package me.darknet.assembler.backend.dalvik;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rules for parameter names in Dalvik bodies. A parameter name is also an identifier of the body, so a name that would
 * silently refer to another binding cannot be used: a duplicate name, {@code this} in an instance method, the positional
 * alias {@code pN} of another parameter, or a numeric register reference such as {@code v0}. The compiler rejects such
 * names, and the printer never prints them.
 */
public final class DalvikParameterNames {
    private DalvikParameterNames() {}

    /**
     * @param name
     * 		Identifier to check, which may be {@code null}.
     *
     * @return {@code true} when the name is a numeric register reference such as {@code v0}.
     */
    public static boolean isNumericRegisterName(@Nullable String name) {
        if (name == null || name.length() < 2 || name.charAt(0) != 'v')
            return false;
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }
        return true;
    }

    /**
     * @param names
     * 		Parameter names in declaration order. A {@code null} or empty entry is unnamed. The list may be shorter than the
     * 		parameter count.
     * @param count
     * 		Number of parameters of the method.
     * @param instance
     * 		{@code true} when the method has a receiver.
     *
     * @return The reason each parameter whose name would collide cannot use it, keyed by parameter index. Empty when no
     * name collides.
     */
    public static @NotNull Map<Integer, String> collisions(@NotNull List<String> names, int count, boolean instance) {
        Map<Integer, String> reasons = new HashMap<>();
        int limit = Math.min(names.size(), count);
        for (int index = 0; index < limit; index++) {
            String name = names.get(index);
            if (name == null || name.isEmpty())
                continue;
            if (isNumericRegisterName(name)) {
                reasons.put(index, "'" + name + "' is a numeric register reference");
                continue;
            }
            if (instance && name.equals("this")) {
                reasons.put(index, "'this' is the receiver of an instance method");
                continue;
            }
            int aliased = parameterWhoseAliasIs(name, index, count);
            if (aliased >= 0) {
                reasons.put(index, "'" + name + "' is the positional alias of parameter " + aliased);
                continue;
            }
            for (int other = 0; other < limit; other++) {
                if (other != index && name.equals(names.get(other))) {
                    reasons.put(index, "'" + name + "' is already the name of parameter " + other);
                    break;
                }
            }
        }
        return reasons;
    }

    /**
     * @return The index of another parameter whose positional alias equals the name, or {@code -1} when there is none.
     */
    private static int parameterWhoseAliasIs(@NotNull String name, int index, int count) {
        for (int other = 0; other < count; other++) {
            if (other != index && name.equals("p" + other))
                return other;
        }
        return -1;
    }
}
