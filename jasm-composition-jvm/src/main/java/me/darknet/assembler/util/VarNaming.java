package me.darknet.assembler.util;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

public class VarNaming {
    public static String name(int index, @NotNull Type type) {
        if (JvmTypeUtils.isPrimitive(type)) {
            // Widen narrow integer primitives to int so fallback variable names stay stable.
            return JvmTypeUtils.widen(type).getDescriptor().toLowerCase() + index;
        }
        return name(index);
    }

    private static String name(int index) {
        return "v" + index;
    }
}
