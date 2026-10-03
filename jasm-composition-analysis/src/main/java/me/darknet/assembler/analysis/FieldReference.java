package me.darknet.assembler.analysis;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a field reference by its owner, name, and descriptor.
 *
 * @param owner
 * 		Class that declares the field.
 * @param name
 * 		Field name.
 * @param descriptor
 * 		Field descriptor.
 */
public record FieldReference(@NotNull String owner, @NotNull String name, @NotNull String descriptor) {
	@Override
	public @NotNull String toString() {
		return owner + "." + name + " " + descriptor;
	}
}
