package me.darknet.assembler.util;

import dev.xdark.blw.code.JavaOpcodes;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BlwOpcodes {

    private static final Map<String, Integer> opcodes = new HashMap<>();
    private static final Map<String, Integer> filteredOpcodes = new HashMap<>();

    public static boolean isVarStore(int opcode) {
        return switch (opcode) {
            case JavaOpcodes.ASTORE,
                    JavaOpcodes.ISTORE,
                    JavaOpcodes.FSTORE,
                    JavaOpcodes.DSTORE,
                    JavaOpcodes.LSTORE -> true;
            default -> false;
        };
    }

    public static int opcode(String name) {
        if (name.endsWith("interface")) {
            String prefix = name.substring(0, name.length() - 9);
            if (prefix.length() == 6)
                return JavaOpcodes.INVOKEINTERFACE;
            else
                return opcodes.get(prefix);
        }
        return opcodes.get(name);
    }

    public static @NotNull Map<String, Integer> getOpcodes() {
        return opcodes;
    }

    public static @NotNull Map<String, Integer> getFilteredOpcodes() {
        return filteredOpcodes;
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
        opcodes.put("line", -1);

        // Filtered
        filteredOpcodes.putAll(opcodes);
        filteredOpcodes.remove("ldc_w");
        filteredOpcodes.remove("ldc2_w");
        filteredOpcodes.remove("jsr");
        filteredOpcodes.remove("ret");
        filteredOpcodes.put("invokestaticinterface", JavaOpcodes.INVOKESTATIC);
        filteredOpcodes.put("invokevirtualinterface", JavaOpcodes.INVOKEINTERFACE);
        filteredOpcodes.put("invokespecialinterface", JavaOpcodes.INVOKESPECIAL);
    }
}
