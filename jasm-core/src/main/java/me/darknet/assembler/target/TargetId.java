package me.darknet.assembler.target;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Canonical identifier for an assembly target.
 *
 * @param value
 * 		Canonical target identifier.
 */
public record TargetId(@NotNull String value) {
    public TargetId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank())
            throw new IllegalArgumentException("Target ID must not be blank");
    }
}