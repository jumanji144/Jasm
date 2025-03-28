package me.darknet.assembler.parser;

import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.dalvik.DalvikInstructions;
import me.darknet.assembler.instructions.jvm.JvmInstructions;

public enum BytecodeFormat {

    JVM(JvmInstructions.INSTANCE),
    DALVIK(DalvikInstructions.INSTANCE);

    public static final BytecodeFormat DEFAULT = JVM;

    private final Instructions<?> instructions;

    BytecodeFormat(Instructions<?> instructions) {
        this.instructions = instructions;
    }

    public Instructions<?> getInstructions() {
        return instructions;
    }
}
