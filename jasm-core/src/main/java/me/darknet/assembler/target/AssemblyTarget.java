package me.darknet.assembler.target;

import org.jetbrains.annotations.NotNull;

/**
 * Bundled or externally supplied assembly target.
 */
public interface AssemblyTarget {
    /**
     * @return Canonical target identifier.
     */
    @NotNull TargetId id();

    /**
     * @return Human-readable target name.
     */
    @NotNull String displayName();

    /**
     * @return Processing services owned by this target.
     */
    @NotNull TargetContext context();
}