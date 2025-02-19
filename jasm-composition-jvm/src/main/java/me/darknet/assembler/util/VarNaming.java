package me.darknet.assembler.util;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;

public class VarNaming {
    public static String name(int index, @NotNull ClassType type) {
        if (type instanceof PrimitiveType prim) {
            // Widen the primitive to an integer as a minimum.
            // This ensures narrow types such as the following are represented as integers.
            //  - boolean
            //  - byte
            //  - short
            //  - char
            // Other types like float/double/long are unaffected.
            // We do this because most contexts will know a primitive is 'int-like' as a baseline
            // but may not know that the content is more narrow like a 'byte'. Thus, if we treat
            // narrower types as 'int' we mitigate that possible disconnect.
            String descriptor = prim.widen(Types.INT).descriptor();
            return descriptor.toLowerCase() + index;
        }
        return name(index);
    }

    private static String name(int index) {
        return "v" + index;
    }
}
