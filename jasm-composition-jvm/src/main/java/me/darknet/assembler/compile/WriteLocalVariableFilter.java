package me.darknet.assembler.compile;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * Per variable filter for local variable debug data.
 *
 * @see JvmCompilerOptions#variableFilter(WriteLocalVariableFilter)
 */
@FunctionalInterface
public interface WriteLocalVariableFilter {

    WriteLocalVariableFilter ALWAYS = (i, name, type) -> true;
    WriteLocalVariableFilter NEVER = (i, name, type) -> false;

    boolean canEmit(int index, @NotNull String name, @NotNull Type type);

}
