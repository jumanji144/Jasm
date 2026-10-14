package me.darknet.assembler.compiler;

import org.jetbrains.annotations.Nullable;

/**
 * Typed compiler output model.
 *
 * @param <V>
 * 		Target representation type.
 */
public interface ClassResult<V> {
    /**
     * @return Target representation, or {@code null} when compilation failed.
     */
    @Nullable V representation();
}
