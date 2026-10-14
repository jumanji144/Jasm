package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;

/**
 * A class type represented by its internal name.
 *
 * @param internalName
 * 		Internal name such as {@code java/lang/String}, without the surrounding {@code L} and {@code ;}.
 */
public record ClassDescriptor(@NotNull String internalName) implements DescriptorType {

    public ClassDescriptor {
        DescriptorParser.requireValidInternalName(internalName);
    }

    @Override
    public @NotNull String descriptor() {
        return 'L' + internalName + ';';
    }
}
