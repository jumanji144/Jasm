package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;

/**
 * An array type of arbitrary dimension.
 *
 * @param component
 * 		Element type of the outermost dimension, which is itself an {@link ArrayDescriptor} for a
 * 		multi-dimensional array.
 */
public record ArrayDescriptor(@NotNull DescriptorType component) implements DescriptorType {

    public ArrayDescriptor {
        if (component instanceof PrimitiveType primitive && primitive.isVoid()) {
            throw new IllegalArgumentException("Array component type must not be void");
        }
    }

    @Override
    public @NotNull String descriptor() {
        return '[' + component.descriptor();
    }
}
