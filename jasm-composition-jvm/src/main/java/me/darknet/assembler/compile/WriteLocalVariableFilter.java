package me.darknet.assembler.compile;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * Per method filter for local variable debug data.
 *
 * @see JvmCompilerOptions#variableFilter(WriteLocalVariableFilter)
 */
@FunctionalInterface
public interface WriteLocalVariableFilter {

    WriteLocalVariableFilter ALWAYS = (name, type) -> true;
    WriteLocalVariableFilter NEVER = (name, type) -> false;

    boolean shouldWriteVariables(@NotNull String name, @NotNull Type type);

}
