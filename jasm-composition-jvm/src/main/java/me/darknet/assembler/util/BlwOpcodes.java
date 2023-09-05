package me.darknet.assembler.util;

import dev.xdark.blw.code.JavaOpcodes;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BlwOpcodes {

    private static final Map<String, Integer> opcodes = new HashMap<>();

    public static int opcode(String name) {
        return opcodes.get(name);
    }

    static {
        Field[] fields = JavaOpcodes.class.getFields();
        for (Field field : fields) {
            try {
                opcodes.put(field.getName().toLowerCase(), field.getInt(null));
            } catch (IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

}
