package me.darknet.assembler.compile.analysis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Basic index-to-name lookup for variable containers.
 */
public interface VariableNameLookup {
    /**
     * @param index
     *              Index of variable.
     *
     * @return Name of variable. Can be {@code null} if the variable is not known.
     */
    @Nullable
    String getVarName(int index);
}
