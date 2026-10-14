package me.darknet.assembler.analysis.func;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code boolean method(char)} executor.
 */
public interface C2ZStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param a
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 */
	boolean apply(char a);

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue a)
			return Values.valueOf(apply((char) a.value()));
		return Values.INT_VALUE;
	}
}
