package me.darknet.assembler.util;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.T_BOOLEAN;

@UtilityClass
public class ArrayTypes {

    public final Map<String, Integer> newArrayTypes = new HashMap<>();

    public boolean isType(String type) {
        return newArrayTypes.containsKey(type);
    }

    public int getType(String type) {
        return newArrayTypes.get(type);
    }

    static {
        newArrayTypes.put("byte", T_BYTE);
        newArrayTypes.put("short", T_SHORT);
        newArrayTypes.put("int", T_INT);
        newArrayTypes.put("long", T_LONG);
        newArrayTypes.put("float", T_FLOAT);
        newArrayTypes.put("double", T_DOUBLE);
        newArrayTypes.put("char", T_CHAR);
        newArrayTypes.put("boolean", T_BOOLEAN);
    }

}
