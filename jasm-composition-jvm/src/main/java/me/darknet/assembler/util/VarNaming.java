package me.darknet.assembler.util;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

public class VarNaming {
    public static String name(int index, @NotNull ClassType type) {
        if (type instanceof PrimitiveType prim)
            return prim.descriptor().toLowerCase() + index;
        return name(index);
    }

    private static String name(int index) {
        return "v" + index;
    }
}
