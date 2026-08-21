package me.darknet.assembler.compile.analysis;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * @param index
 * 		Variable slot index.
 * @param name
 * 		Variable name.
 * @param type
 * 		Inferred or declared source type.
 * @param startIndex
 * 		Inclusive instruction-list index where the range starts.
 * @param endIndex
 * 		Exclusive instruction-list index where the range ends.
 */
public record LocalVariableState(
		int index,
		@NotNull String name,
		@NotNull Type type,
		int startIndex,
		int endIndex) {}
