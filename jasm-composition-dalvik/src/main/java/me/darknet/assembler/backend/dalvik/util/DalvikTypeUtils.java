package me.darknet.assembler.backend.dalvik.util;

import me.darknet.assembler.descriptor.DescriptorType;
import org.jetbrains.annotations.Nullable;

import static me.darknet.assembler.descriptor.PrimitiveType.DOUBLE;
import static me.darknet.assembler.descriptor.PrimitiveType.LONG;

/**
 * Provides conversions and queries for Dalvik descriptors, types, and type paths.
 */
public class DalvikTypeUtils {
	/**
	 * @param type
	 * 		The type to check.
	 *
	 * @return {@code true} if the type is a wide primitive type, {@code false} otherwise.
	 */
	public static boolean isWideType(@Nullable DescriptorType type) {
		// TODO: Surely we've already written this method somewhere else...
		return type == LONG || type == DOUBLE;
	}
}
