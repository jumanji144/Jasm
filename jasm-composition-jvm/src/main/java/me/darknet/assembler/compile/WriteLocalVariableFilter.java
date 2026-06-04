package me.darknet.assembler.compile;

import dev.xdark.blw.type.MethodType;
import org.jetbrains.annotations.NotNull;

/**
 * Per method filter for local variable debug data.
 *
 * @see JvmCompilerOptions#variableFilter(WriteLocalVariableFilter)
 */
@FunctionalInterface
public interface WriteLocalVariableFilter {

    WriteLocalVariableFilter ALWAYS = (name, type) -> true;
    WriteLocalVariableFilter NEVER = (name, type) -> false;

    boolean shouldWriteVariables(@NotNull String name, @NotNull MethodType type);

}
