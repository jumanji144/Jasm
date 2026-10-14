package me.darknet.assembler.compiler;

import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.ValidatedUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Compiler boundary for one target representation.
 *
 * @param <O>
 * 		Target compiler options.
 * @param <V>
 * 		Target class representation.
 * @param <R>
 * 		Target compile result.
 */
public interface Compiler<O extends CompilerOptions<O, V>, V, R extends ClassResult<V>> {
	/**
	 * @param unit
	 * 		Semantically validated unit to compile. This type guarantees that semantic processing completed without errors.
	 * @param options
	 * 		Target compiler options.
	 *
	 * @return Typed compilation result.
	 */
	@NotNull Outcome<R> compile(@NotNull ValidatedUnit unit, @NotNull O options);
}
