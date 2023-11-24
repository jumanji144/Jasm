package me.darknet.assembler.compile.analysis.frame;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.Local;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Frame that tracks only type info of stack/local entries.
 */
public non-sealed interface TypedFrame extends Frame {
	/**
	 * @return Stack.
	 */
	@NotNull
	Deque<ClassType> getStack();

	/**
	 * @return Map of local variables.
	 */
	@NotNull
	Map<Integer, Local> getLocals();

	@Override
	default @NotNull Stream<Local> locals() {
		return getLocals().values().stream();
	}

	/**
	 * @param index
	 * 		Index of variable to get.
	 *
	 * @return Variable, or {@code null} if not a known variable.
	 */
	@Nullable
	default Local getLocal(int index) {
		return getLocals().get(index);
	}

	@Nullable
	@Override
	default ClassType getLocalType(int index) {
		Local local = getLocals().get(index);
		if (local == null)
			return null;
		return local.type();
	}

	/**
	 * @param index
	 * 		Index of variable to assign.
	 * @param local
	 * 		Variable info to assign.
	 */
	void setLocal(int index, @NotNull Local local);

	/**
	 * Removes the top item from the stack, returning that value.
	 *
	 * @return Top type on the stack.
	 */
	@NotNull
	ClassType pop();

	/**
	 * @return Top type <i>(offset by one)</i> on the stack.
	 */
	@NotNull
	default ClassType pop2() {
		pop();
		return pop();
	}

	/**
	 * @param type
	 * 		Type to pop off the stack.
	 */
	@NotNull
	default ClassType pop(@NotNull ClassType type) {
		if (type == Types.LONG || type == Types.DOUBLE) {
			return pop2();
		} else {
			return pop();
		}
	}

	/**
	 * @return Top type on the stack.
	 */
	@NotNull
	ClassType peek();
}
