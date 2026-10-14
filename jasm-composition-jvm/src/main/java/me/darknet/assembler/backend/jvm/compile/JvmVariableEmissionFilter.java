package me.darknet.assembler.backend.jvm.compile;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * Selects which local-variable entries are emitted in a generated JVM method's debug table.
 *
 * @see JvmCompilerOptions#withVariableFilter(JvmVariableEmissionFilter)
 */
@FunctionalInterface
public interface JvmVariableEmissionFilter {
	/** Filter that always emits debug information for local variables. */
	JvmVariableEmissionFilter ALWAYS = (i, name, type) -> true;
	/** Filter that never emits debug information for local variables. */
	JvmVariableEmissionFilter NEVER = (i, name, type) -> false;

	/**
	 * Tests whether debug information should be emitted for a local variable.
	 *
	 * @param index
	 * 		Local-variable slot index.
	 * @param name
	 * 		Local-variable name.
	 * @param type
	 * 		Local-variable type.
	 *
	 * @return {@code true} if debug information should be emitted for the local variable, {@code false} otherwise.
	 */
	boolean canEmit(int index, @NotNull String name, @NotNull Type type);
}
