package me.darknet.assembler.analysis.func;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code long method(long)} executor.
 */
public interface J2JStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param d
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 */
	long apply(long d);

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue a)
			return Values.valueOf(apply(a.value()));
		return Values.LONG_VALUE;
	}
}
