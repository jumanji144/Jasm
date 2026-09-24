package me.darknet.assembler.target;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable insertion-ordered registry of assembly targets.
 */
public final class TargetRegistry {
    private final List<AssemblyTarget> targets;
    private final List<TargetId> ids;

    private TargetRegistry(List<AssemblyTarget> targets) {
        this.targets = List.copyOf(targets);
        this.ids = this.targets.stream().map(AssemblyTarget::id).toList();
    }

    /**
     * @param targets
     * 		Targets in canonical insertion order.
     *
     * @return Immutable target registry.
     *
     * @throws IllegalArgumentException
     * 		If duplicate canonical IDs are supplied.
     */
    public static @NotNull TargetRegistry of(@NotNull List<? extends AssemblyTarget> targets) {
        Objects.requireNonNull(targets, "targets");
        List<AssemblyTarget> copy = new ArrayList<>(targets.size());
        Set<TargetId> seen = new HashSet<>();
        for (AssemblyTarget target : targets) {
            Objects.requireNonNull(target, "targets contains null");
            TargetId id = Objects.requireNonNull(target.id(), "target.id");
            if (!seen.add(id))
                throw new IllegalArgumentException("Duplicate target ID: " + id.value());
            copy.add(target);
        }
        return new TargetRegistry(copy);
    }

    /**
     * @return Immutable canonical IDs in insertion order.
     */
    public @NotNull List<TargetId> ids() {
        return ids;
    }

    /**
     * @param id
     * 		Canonical or case-insensitive CLI target ID.
     *
     * @return Matching target, or {@code null} when no target matches.
     */
    public @Nullable AssemblyTarget find(@NotNull TargetId id) {
        Objects.requireNonNull(id, "id");
        String requested = id.value().toLowerCase(Locale.ROOT);
        for (AssemblyTarget target : targets) {
            if (target.id().value().toLowerCase(Locale.ROOT).equals(requested))
                return target;
        }
        return null;
    }
}