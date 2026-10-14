package me.darknet.assembler.backend.jvm.util;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import java.util.Objects;
import java.util.UUID;

/**
 * Provides conversions and queries for JVM descriptors, types, and type paths.
 */
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

	/**
	 * @return Package name for internal marker types.
	 */
	private static String getPackage() {
		return debug ? DEBUG_PACKAGE : RUNTIME_PACKAGE;
	}

	/**
	 * @param type
	 * 		Class to convert.
	 *
	 * @return ASM type corresponding to the given class.
	 */
	public static @NotNull Type type(@NotNull Class<?> type) {
		return Type.getType(type);
	}

	/**
	 * @param internalName
	 * 		Internal name of the class.
	 *
	 * @return Object type with the given internal name.
	 */
	public static @NotNull Type objectType(@NotNull String internalName) {
		return Type.getObjectType(internalName);
	}

	/**
	 * @param componentType
	 * 		Component type of the array.
	 *
	 * @return Array type with the given component type.
	 */
	public static @NotNull Type arrayType(@NotNull Type componentType) {
		return Type.getType('[' + componentType.getDescriptor());
	}

	/**
	 * @param type
	 * 		ASM type to convert.
	 *
	 * @return Descriptor type corresponding to the given ASM type.
	 *
	 * @throws IllegalArgumentException
	 * 		If the given type is not an ordinary JVM descriptor type.
	 */
	public static @NotNull DescriptorType toDescriptorType(@NotNull Type type) {
		if (isTop(type) || isNullMarker(type) || isUninitialized(type))
			throw new IllegalArgumentException("Not an ordinary JVM descriptor type: " + type);
		return switch (type.getSort()) {
			case Type.VOID, Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG,
			     Type.DOUBLE -> Objects.requireNonNull(PrimitiveType.fromDescriptor(type.getDescriptor().charAt(0)));
			case Type.OBJECT -> new ClassDescriptor(type.getInternalName());
			case Type.ARRAY -> {
				DescriptorType component = toDescriptorType(type.getElementType());
				for (int i = 0; i < type.getDimensions(); i++)
					component = new ArrayDescriptor(component);
				yield (ArrayDescriptor) component;
			}
			default -> throw new IllegalArgumentException("Not an ordinary JVM descriptor type: " + type);
		};
	}

	/**
	 * @param type
	 * 		Descriptor type to convert.
	 *
	 * @return ASM type corresponding to the given descriptor type.
	 */
	public static @NotNull Type toAsmType(@NotNull DescriptorType type) {
		return toAsmType(type.descriptor());
	}

	/**
	 * @param descriptor
	 * 		Descriptor to convert.
	 *
	 * @return ASM type corresponding to the given descriptor.
	 */
	public static @NotNull Type toAsmType(@NotNull String descriptor) {
		return Type.getType(descriptor);
	}

	/**
	 * @param descriptor
	 * 		Method descriptor to convert.
	 *
	 * @return ASM method type corresponding to the given descriptor.
	 */
	public static @NotNull Type toAsmMethodType(@NotNull String descriptor) {
		return Type.getMethodType(descriptor);
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is a reference type.
	 */
	public static boolean isReference(@Nullable Type type) {
		return type != null && !isTop(type) && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is a primitive type.
	 */
	public static boolean isPrimitive(@Nullable Type type) {
		return type != null && !isTop(type) && switch (type.getSort()) {
			case Type.VOID, Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG,
			     Type.DOUBLE -> true;
			default -> false;
		};
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is a wide primitive type.
	 */
	public static boolean isWide(@Nullable Type type) {
		return LONG.equals(type) || DOUBLE.equals(type);
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is a top marker type.
	 */
	public static boolean isTop(@Nullable Type type) {
		return TOP.equals(type);
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is a null marker type.
	 */
	public static boolean isNullMarker(@Nullable Type type) {
		return NULL.equals(type);
	}

	/**
	 * @param owner
	 * 		Owner type of the uninitialized reference.
	 * @param identity
	 * 		Identity of the uninitialized reference.
	 *
	 * @return Uninitialized reference type with the given owner and identity.
	 */
	public static @NotNull Type uninitializedType(@NotNull Type owner, int identity) {
		return Type.getObjectType(UNINITIALIZED_PREFIX + identity + "$" + internalName(owner));
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is an uninitialized reference type.
	 */
	public static boolean isUninitialized(@Nullable Type type) {
		return type != null && type.getSort() == Type.OBJECT
				&& type.getInternalName().startsWith(UNINITIALIZED_PREFIX);
	}

	/**
	 * @param type
	 * 		Uninitialized type to query.
	 *
	 * @return Identity of the given uninitialized type.
	 */
	public static int uninitializedIdentity(@NotNull Type type) {
		if (!isUninitialized(type))
			throw new IllegalArgumentException("Not an uninitialized type: " + type);
		String internalName = type.getInternalName();
		int separator = internalName.indexOf('$', UNINITIALIZED_PREFIX.length());
		if (separator < 0)
			throw new IllegalArgumentException("Malformed uninitialized type: " + type);
		return Integer.parseInt(internalName.substring(UNINITIALIZED_PREFIX.length(), separator));
	}

	/**
	 * @param type
	 * 		Uninitialized type to query.
	 *
	 * @return Owner type of the given uninitialized type.
	 */
	public static @NotNull Type uninitializedOwner(@NotNull Type type) {
		if (!isUninitialized(type))
			throw new IllegalArgumentException("Not an uninitialized type: " + type);
		String internalName = type.getInternalName();
		int separator = internalName.indexOf('$', UNINITIALIZED_PREFIX.length());
		if (separator < 0 || separator + 1 >= internalName.length())
			throw new IllegalArgumentException("Malformed uninitialized type: " + type);
		return Type.getObjectType(internalName.substring(separator + 1));
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is a primitive that
	 * can be widened to an {@code int} for verification purposes.
	 */
	public static boolean isIntegerLike(@Nullable Type type) {
		return type != null && switch (type.getSort()) {
			case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> true;
			default -> false;
		};
	}

	/**
	 * @param type
	 * 		Type to convert.
	 *
	 * @return Verification type corresponding to the given type, or {@link #INT} for integer-like types.
	 */
	public static @Nullable Type verificationType(@Nullable Type type) {
		if (type == null || isTop(type))
			return type;
		return isIntegerLike(type) ? INT : type;
	}

	/**
	 * @param type
	 * 		Type to convert.
	 *
	 * @return Category of the given type.
	 * Wide {@code long} and {@code double} types have category 2, all other types have category 1.
	 */
	public static int category(@Nullable Type type) {
		return isWide(type) ? 2 : 1;
	}

	/**
	 * @param type
	 * 		Type to convert.
	 *
	 * @return Display name of the given type, or "null" for null types.
	 */
	public static @NotNull String displayName(@Nullable Type type) {
		return type == null ? "null" : type.getDescriptor();
	}

	/**
	 * @param type
	 * 		Type to convert.
	 *
	 * @return Internal name of the given type, or descriptor for array types.
	 */
	public static @NotNull String internalName(@NotNull Type type) {
		return type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
	}

	/**
	 * @param type
	 * 		Type to widen.
	 *
	 * @return Widened type, or the same type if not a primitive that can be widened.
	 */
	public static @NotNull Type widen(@NotNull Type type) {
		return switch (type.getSort()) {
			case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT -> INT;
			default -> type;
		};
	}

	/**
	 * @param opcode
	 * 		Opcode to convert.
	 *
	 * @return Primitive type corresponding to the given load/store opcode, or {@link #OBJECT} for reference types.
	 */
	public static @NotNull Type primitiveFromStoreOpcode(int opcode) {
		return switch (opcode) {
			case Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IINC, Opcodes.RET -> INT;
			case Opcodes.LLOAD, Opcodes.LSTORE -> LONG;
			case Opcodes.FLOAD, Opcodes.FSTORE -> FLOAT;
			case Opcodes.DLOAD, Opcodes.DSTORE -> DOUBLE;
			default -> OBJECT;
		};
	}

	/**
	 * @param checker
	 * 		Inheritance checker to use for reference types.
	 * @param a
	 * 		First type.
	 * @param b
	 * 		Second type.
	 *
	 * @return Common type, or {@code null} if incompatible.
	 */
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

	/**
	 * Converts a source type path into an ASM {@link TypePath}.
	 *
	 * @param typePath
	 * 		Source type path, or {@code null} when the annotation has no path.
	 *
	 * @return Parsed type path, or {@code null} when absent or written as the root placeholder {@code _}.
	 */
	public static @Nullable TypePath parseTypePath(@Nullable ASTIdentifier typePath) {
		if (typePath == null)
			return null;
		String content = typePath.content();
		return "_".equals(content) ? null : TypePath.fromString(content);
	}
}
