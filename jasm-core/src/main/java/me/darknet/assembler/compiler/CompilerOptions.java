package me.darknet.assembler.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Target-owned compiler options with a typed overlay representation.
 *
 * @param <B>
 * 		Concrete options type.
 * @param <V>
 * 		Target representation type.
 */
public interface CompilerOptions<B extends CompilerOptions<B, V>, V> {
    /**
     * @param representation
     * 		Representation to overlay, or {@code null} to clear the overlay.
     *
     * @return This options object.
     */
    @NotNull B withOverlay(@Nullable V representation);

    /**
     * @return Configured overlay representation, or {@code null} when absent.
     */
    @Nullable V getOverlay();
}
