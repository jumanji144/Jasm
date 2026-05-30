package me.darknet.assembler.util;

import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class JvmTypeUtils {
    public static final Type VOID = Type.VOID_TYPE;
    public static final Type BOOLEAN = Type.BOOLEAN_TYPE;
    public static final Type CHAR = Type.CHAR_TYPE;
    public static final Type BYTE = Type.BYTE_TYPE;
    public static final Type SHORT = Type.SHORT_TYPE;
    public static final Type INT = Type.INT_TYPE;
    public static final Type FLOAT = Type.FLOAT_TYPE;
    public static final Type LONG = Type.LONG_TYPE;
    public static final Type DOUBLE = Type.DOUBLE_TYPE;
    public static final Type OBJECT = Type.getObjectType("java/lang/Object");
    public static final Type STRING = Type.getObjectType("java/lang/String");
    public static final Type CLASS = Type.getObjectType("java/lang/Class");
    public static final Type METHOD_TYPE = Type.getObjectType("java/lang/invoke/MethodType");
    public static final Type METHOD_HANDLE = Type.getObjectType("java/lang/invoke/MethodHandle");
    public static final Type BOX_VOID = Type.getObjectType("java/lang/Void");

    private JvmTypeUtils() {
    }

    public static @NotNull Type type(@NotNull Class<?> type) {
        return Type.getType(type);
    }

    public static @NotNull Type objectType(@NotNull String internalName) {
        return Type.getObjectType(internalName);
    }

    public static @NotNull Type arrayType(@NotNull Type componentType) {
        return Type.getType('[' + componentType.getDescriptor());
    }

    public static @NotNull Type typeFromDescriptor(@NotNull String descriptor) {
        return Type.getType(descriptor);
    }

    public static @NotNull Type methodType(@NotNull String descriptor) {
        return Type.getMethodType(descriptor);
    }

    public static boolean isReference(@Nullable Type type) {
        return type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
    }

    public static boolean isPrimitive(@Nullable Type type) {
        return type != null && switch (type.getSort()) {
            case Type.VOID, Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE -> true;
            default -> false;
        };
    }

    public static boolean isWide(@Nullable Type type) {
        return type == LONG || type == DOUBLE;
    }

    public static int category(@Nullable Type type) {
        return isWide(type) ? 2 : 1;
    }

    public static @NotNull String displayName(@Nullable Type type) {
        return type == null ? "null" : type.getDescriptor();
    }

    public static @NotNull String internalName(@NotNull Type type) {
        return type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
    }

    public static @NotNull Type elementType(@NotNull Type arrayType) {
        return arrayType.getElementType();
    }

    public static @NotNull Type widen(@NotNull Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT -> INT;
            default -> type;
        };
    }

    public static @NotNull Type primitiveFromStoreOpcode(int opcode) {
        return switch (opcode) {
            case Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IINC, Opcodes.RET -> INT;
            case Opcodes.LLOAD, Opcodes.LSTORE -> LONG;
            case Opcodes.FLOAD, Opcodes.FSTORE -> FLOAT;
            case Opcodes.DLOAD, Opcodes.DSTORE -> DOUBLE;
            default -> OBJECT;
        };
    }

    public static @Nullable Type commonType(@NotNull InheritanceChecker checker, @Nullable Type a, @Nullable Type b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        if (a.equals(b)) return a;
        if (isPrimitive(a) && isPrimitive(b))
            return widen(a).equals(widen(b)) ? widen(a) : INT;

        if (isReference(a) && isReference(b)) {
            String commonSuperclass = checker.getCommonSuperclass(internalName(a), internalName(b));
            return commonSuperclass == null ? OBJECT : objectType(commonSuperclass);
        }
        return OBJECT;
    }
}
