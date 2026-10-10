package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single-character descriptor types of JVM and Dalvik primitives.
 */
public enum PrimitiveType implements DescriptorType {
    BYTE('B', "byte"),
    CHAR('C', "char"),
    DOUBLE('D', "double"),
    FLOAT('F', "float"),
    INT('I', "int"),
    LONG('J', "long"),
    SHORT('S', "short"),
    BOOLEAN('Z', "boolean"),
    VOID('V', "void");

    private final char descriptor;
    private final String javaName;

    PrimitiveType(char descriptor, String javaName) {
        this.descriptor = descriptor;
        this.javaName = javaName;
    }

    /**
     * @param descriptor
     * 		Descriptor character to resolve.
     *
     * @return Matching primitive, or {@code null} when the character is not a primitive descriptor.
     */
    public static @Nullable PrimitiveType fromDescriptor(char descriptor) {
        for (PrimitiveType type : values()) {
            if (type.descriptor == descriptor) {
                return type;
            }
        }
        return null;
    }

    /**
     * @param name
     * 		Java-style name such as {@code int} or {@code boolean}.
     *
     * @return Matching primitive, or {@code null} when the name is not a primitive name.
     */
    public static @Nullable PrimitiveType fromJavaName(@NotNull String name) {
        for (PrimitiveType type : values()) {
            if (type.javaName.equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * @return {@code true} for {@link #VOID}.
     */
    public boolean isVoid() {
        return this == VOID;
    }

    @Override
    public @NotNull String descriptor() {
        return String.valueOf(descriptor);
    }
}
