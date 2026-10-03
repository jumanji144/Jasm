package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;

import static me.darknet.assembler.descriptor.PrimitiveType.*;

/**
 * Utility class for working with descriptors.
 */
public final class Descriptors {
	private Descriptors() {}

	public static boolean isIntLike(@NotNull DescriptorType type) {
		return type == BOOLEAN || type == BYTE || type == CHAR || type == SHORT || type == INT;
	}

	public static boolean isReferenceType(@NotNull DescriptorType type) {
		return type instanceof ClassDescriptor || type instanceof ArrayDescriptor;
	}
}
