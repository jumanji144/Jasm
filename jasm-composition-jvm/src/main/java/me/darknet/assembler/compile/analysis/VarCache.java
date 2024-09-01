package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Variable cache. Used for name/index lookups.
 *
 * @see VarCacheUpdater
 */
public class VarCache {
    private final List<Variable> variables = new ArrayList<>();
    private int nextAvailableIndex = 0;

    /**
     * Get or create a variable by the given name.
     *
     * @param name
     *         Variable name.
     * @param wide
     *         Variable type wide flag <i>({@code true} for {@code long}/{@code double})</i>.
     *
     * @return Index of variable by name.
     */
    public int getOrCreate(@NotNull String name, boolean wide) {
        // Get existing variable
        Variable variable = getFirstByName(name);
        if (variable != null)
            return variable.index;

        // Compute next available index
        variable = getOrCreate(name, nextAvailableIndex, wide);
        return variable.index;
    }

    /**
     * Get or create a variable by the given name. If it does not exist, create it at the given index.
     *
     * @param name
     *         Variable name.
     * @param index
     *         Variable index.
     * @param wide
     *         Variable type wide flag <i>({@code true} for {@code long}/{@code double})</i>.
     *
     * @return Existing or newly created variable.
     */
    public @NotNull Variable getOrCreate(@NotNull String name, int index, boolean wide) {
        Variable variable = getFirstByName(name);
        if (variable == null) {
            variable = new Variable(name, index, wide);
            variables.add(variable);
            nextAvailableIndex = Math.max(nextAvailableIndex, index + (wide ? 2 : 1));
        }
        return variable;
    }

    /**
     * @param name
     *         Variable name.
     *
     * @return First matching variable. {@code null} for no match.
     */
    private @Nullable Variable getFirstByName(@NotNull String name) {
        return variables.stream()
                .filter(v -> Objects.equals(v.name, name))
                .findFirst().orElse(null);
    }

    /**
     * @param index
     *         Variable index.
     *
     * @return First matching variable. {@code null} for no match.
     */
    public @Nullable Variable getFirstByIndex(int index) {
        if (index < 0)
            return null;
        return variables.stream()
                .filter(v -> v.index == index)
                .findFirst().orElse(null);
    }

    /**
     * @param index
     *         Index of variable.
     *
     * @return Name of variable. Can be {@code null} if the variable is not known.
     */
    public @Nullable String getVarName(int index) {
        Variable variable = getFirstByIndex(index);
        if (variable == null) return null;
        return variable.name;
    }

    /**
     * @param offset
     *         Code offset.
     *
     * @return Stream of variables in-scope at the given offset.
     */
    public @NotNull Stream<Variable> varsAtOffset(int offset) {
        return variables.stream()
                .filter(v -> v.firstAssigned <= offset);
    }

    /**
     * Variable model.
     */
    public static class Variable {
        private final String name;
        private final int index;
        private final boolean wide;
        private ClassType typeHint;
        private int firstAssigned = Integer.MAX_VALUE;

        public Variable(String name, int index, boolean wide) {
            this.name = name;
            this.index = index;
            this.wide = wide;
        }

        public void updateFirstAssignedOffset(int offset) {
            // Move the first assigned offset earlier.
            if (offset < firstAssigned)
                firstAssigned = offset;
        }

        public void updateTypeHint(@Nullable ClassType typeHint) {
            this.typeHint = typeHint;
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }

        @Nullable
        public ClassType getTypeHint() {
            return typeHint;
        }

        @Override
        public String toString() {
            return "var " + name + "[" + index + "]" + (wide ? "w" : "") + " <-- " + firstAssigned;
        }
    }
}
