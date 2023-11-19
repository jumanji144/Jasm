package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import org.jetbrains.annotations.NotNull;

/**
 * Outline of a stack entry in a frame.
 */
public interface StackEntry {
	/**
	 * @return Type of this stack value.
	 */
	@NotNull
	ClassType type();
}
