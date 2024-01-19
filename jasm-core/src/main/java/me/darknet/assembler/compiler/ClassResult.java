package me.darknet.assembler.compiler;

import org.jetbrains.annotations.Nullable;

/**
 * Compiler output model.
 */
public interface ClassResult {
    @Nullable
    ClassRepresentation representation();
}
