package me.darknet.assembler.analysis;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a method reference by its owner, name, and descriptor.
 *
 * @param owner
 * 		Class that declares the method.
 * @param name
 * 		Method name.
 * @param descriptor
 * 		Method descriptor.
 */
public record MethodReference(@NotNull String owner, @NotNull String name, @NotNull String descriptor) {
	@Override
	public String toString() {
		return owner + "." + name + descriptor;
	}
}
