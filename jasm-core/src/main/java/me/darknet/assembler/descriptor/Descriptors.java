package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.darknet.assembler.descriptor.PrimitiveType.*;

/**
 * Utility class for working with descriptors.
 */
public final class Descriptors {
	private Descriptors() {}

	/**
	 * @param type
	 * 		Descriptor type to check.
	 *
	 * @return {@code true} if the descriptor type is a non-void primitive type that can be represented as an integer.
	 */
	public static boolean isIntLike(@NotNull DescriptorType type) {
		return type == BOOLEAN || type == BYTE || type == CHAR || type == SHORT || type == INT;
	}

	/**
	 * @param type
	 * 		Descriptor type to check.
	 *
	 * @return {@code true} if the descriptor type is a reference type (class or array).
	 */
	public static boolean isReferenceType(@NotNull DescriptorType type) {
		return type instanceof ClassDescriptor || type instanceof ArrayDescriptor;
	}

	/**
	 * @param descriptor
	 * 		Field descriptor to classify.
	 *
	 * @return Primitive category, or {@code null} when the descriptor is not a non-void primitive.
	 */
	public static @Nullable PrimitiveType primitiveCategory(@NotNull String descriptor) {
		if (descriptor.length() != 1)
			return null;
		PrimitiveType primitive = PrimitiveType.fromDescriptor(descriptor.charAt(0));
		if (primitive == null || primitive.isVoid())
			return null;
		return isIntLike(primitive) ? INT : primitive;
	}

	/**
	 * @param type
	 * 		Descriptor type to check.
	 *
	 * @return {@code true} if the descriptor type is the wide primitive {@code long} or {@code double}.
	 */
	public static boolean isWideType(@Nullable DescriptorType type) {
		return type == LONG || type == DOUBLE;
	}

	/**
	 * @param descriptor
	 * 		Field descriptor to check.
	 *
	 * @return {@code true} if the descriptor is a wide primitive ({@code J} or {@code D}).
	 */
	public static boolean isWideType(@NotNull String descriptor) {
		return descriptor.equals("J") || descriptor.equals("D");
	}

	/**
	 * Returns the descriptor unchanged when it has the array descriptor prefix.
	 *
	 * @param descriptor
	 * 		Field descriptor to check.
	 *
	 * @return The same descriptor when it starts with {@code [}, otherwise {@code null}.
	 */
	public static @Nullable String arrayDescriptor(@NotNull String descriptor) {
		return descriptor.startsWith("[") ? descriptor : null;
	}
}
