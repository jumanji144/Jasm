package me.darknet.assembler.util;

import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.UUID;

public class JvmTypeUtils {
    public static boolean debug = false; // To prevent abuse, we generate a random package name for non-debug runs.
    private static final String DEBUG_PACKAGE = "jasm/analysis/";
    private static final String RUNTIME_PACKAGE = UUID.randomUUID() + "/";
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
    /** Internal marker used for an undefined local or an invalid/unknown verifier slot. */
    public static final Type TOP = Type.getObjectType(getPackage() + "TOP");
    /** Internal marker used for a known null stack value. */
    public static final Type NULL = Type.getObjectType(getPackage() + "NULL");
    private static final String UNINITIALIZED_PREFIX = getPackage() + "UNINITIALIZED$";

    private JvmTypeUtils() {}

    private static String getPackage() {
        return debug ? DEBUG_PACKAGE : RUNTIME_PACKAGE;
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
        return type != null && !isTop(type) && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
    }

    public static boolean isPrimitive(@Nullable Type type) {
        return type != null && !isTop(type) && switch (type.getSort()) {
            case Type.VOID, Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE -> true;
            default -> false;
        };
    }

    public static boolean isWide(@Nullable Type type) {
        return LONG.equals(type) || DOUBLE.equals(type);
    }

    public static boolean isTop(@Nullable Type type) {
        return TOP.equals(type);
    }

    public static boolean isNullMarker(@Nullable Type type) {
        return NULL.equals(type);
    }

    public static @NotNull Type uninitializedType(@NotNull Type owner, int identity) {
        return Type.getObjectType(UNINITIALIZED_PREFIX + identity + "$" + internalName(owner));
    }

    public static boolean isUninitialized(@Nullable Type type) {
        return type != null && type.getSort() == Type.OBJECT
                && type.getInternalName().startsWith(UNINITIALIZED_PREFIX);
    }

    public static @NotNull Type uninitializedOwner(@NotNull Type type) {
        if (!isUninitialized(type))
            throw new IllegalArgumentException("Not an uninitialized type: " + type);
        String internalName = type.getInternalName();
        int separator = internalName.indexOf('$', UNINITIALIZED_PREFIX.length());
        if (separator < 0 || separator + 1 >= internalName.length())
            throw new IllegalArgumentException("Malformed uninitialized type: " + type);
        return Type.getObjectType(internalName.substring(separator + 1));
    }

    public static boolean isIntegerLike(@Nullable Type type) {
        return type != null && switch (type.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> true;
            default -> false;
        };
    }

    public static @Nullable Type verificationType(@Nullable Type type) {
        if (type == null || isTop(type))
            return type;
        return isIntegerLike(type) ? INT : type;
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
        if (isTop(a) || isTop(b)) return TOP;
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        if (a.equals(b)) return a;
        if (isPrimitive(a) && isPrimitive(b)) {
            Type va = verificationType(a);
            Type vb = verificationType(b);
            return va.equals(vb) ? va : null;
        }

        if (isReference(a) && isReference(b)) {
			if (a.getSort() == Type.ARRAY || b.getSort() == Type.ARRAY) {
				if (a.getSort() != Type.ARRAY || b.getSort() != Type.ARRAY)
					return OBJECT;
				Type component = commonType(checker, a.getElementType(), b.getElementType());
				if (component == null || isTop(component) || isPrimitive(component)
						&& !verificationType(a.getElementType()).equals(verificationType(b.getElementType())))
					return OBJECT;
				return arrayType(component);
			}
            String commonSuperclass = checker.getCommonSuperclass(internalName(a), internalName(b));
            return commonSuperclass == null ? OBJECT : objectType(commonSuperclass);
        }
        return null;
    }
}
