package me.darknet.assembler.util;

import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

public class Handles {

    private static final Map<String, Integer> handleTypes = new HashMap<>();

    static {
        handleTypes.put("H_GETFIELD", Opcodes.H_GETFIELD);
        handleTypes.put("H_GETSTATIC", Opcodes.H_GETSTATIC);
        handleTypes.put("H_PUTFIELD", Opcodes.H_PUTFIELD);
        handleTypes.put("H_PUTSTATIC", Opcodes.H_PUTSTATIC);
        handleTypes.put("H_INVOKEVIRTUAL", Opcodes.H_INVOKEVIRTUAL);
        handleTypes.put("H_INVOKESTATIC", Opcodes.H_INVOKESTATIC);
        handleTypes.put("H_INVOKESPECIAL", Opcodes.H_INVOKESPECIAL);
        handleTypes.put("H_NEWINVOKESPECIAL", Opcodes.H_NEWINVOKESPECIAL);
        handleTypes.put("H_INVOKEINTERFACE", Opcodes.H_INVOKEINTERFACE);
    }

    public static boolean isValid(String name) {
        return handleTypes.containsKey(name);
    }

    public static int getType(String handleType) {
        return handleTypes.get(handleType);
    }

}
