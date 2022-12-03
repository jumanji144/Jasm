package me.darknet.assembler.instructions;

import me.darknet.assembler.parser.groups.instructions.HandleGroup;
import me.darknet.assembler.parser.groups.instructions.TypeGroup;

public enum Argument {

    /**
     * any case of identifier
     */
    NAME,
    /**
     * Special case of {@link #NAME} for class names
     */
    CLASS,
    /**
     * Special case of {@link #NAME} for field references.<br>
     * Consists of {@link #CLASS}.{@link #NAME}
     */
    FIELD,
    /**
     * Special case of {@link #NAME} for method references.<br>
     * Consists of {@link #CLASS}.{@link #NAME}
     */
    METHOD,
    /**
     * Special case of {@link #NAME} for any type of type descriptors
     */
    DESCRIPTOR,
    INDEX,
    /**
     * Meta type being able to have anything as value including {@link HandleGroup} and
     * {@link TypeGroup}
     */
    CONSTANT, // extension to NAME including handle, type and other objects
    LABEL,
    BYTE,
    SHORT,
    INTEGER,
    // SPECIAL
    SWITCH,
    BOOTSTRAP_ARGUMENTS,
    HANDLE,
    TYPE,
    OPTIONAL;

    private Argument subType;

    public static Argument optional(Argument subType) {
        Argument arg = OPTIONAL;
        arg.subType = subType;
        return arg;
    }

}
