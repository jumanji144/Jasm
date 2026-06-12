package me.darknet.assembler.compile;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * Per variable filter for local variable debug data.
 *
 * @see JvmCompilerOptions#variableFilter(JvmVariableEmissionFilter)
 */
@FunctionalInterface
public interface JvmVariableEmissionFilter {

    JvmVariableEmissionFilter ALWAYS = (i, name, type) -> true;
    JvmVariableEmissionFilter NEVER = (i, name, type) -> false;

    boolean canEmit(int index, @NotNull String name, @NotNull Type type);

}
