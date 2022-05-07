package me.darknet.assembler.util;

import org.objectweb.asm.Opcodes;

import java.util.Map;

public class Handles {

    private static final Map<String, Integer> handleTypes = Map.of(
            "H_GETFIELD", Opcodes.H_GETFIELD,
            "H_GETSTATIC", Opcodes.H_GETSTATIC,
            "H_PUTFIELD", Opcodes.H_PUTFIELD,
            "H_PUTSTATIC", Opcodes.H_PUTSTATIC,
            "H_INVOKEVIRTUAL", Opcodes.H_INVOKEVIRTUAL,
            "H_INVOKESTATIC", Opcodes.H_INVOKESTATIC,
            "H_INVOKESPECIAL", Opcodes.H_INVOKESPECIAL,
            "H_NEWINVOKESPECIAL", Opcodes.H_INVOKESPECIAL,
            "H_INVOKEINTERFACE", Opcodes.H_INVOKEINTERFACE
    );

    public static boolean isValid(String name) {
        return handleTypes.containsKey(name);
    }

    public static int getType(String handleType) {
        return handleTypes.get(handleType);
    }

}
