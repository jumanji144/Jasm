package me.darknet.assembler.analysis.func;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(long)} executor.
 */
public interface J2IStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param d
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 *
	 * @throws ArithmeticException
	 * 		If the operation cannot be applied to the supplied arguments.
	 */
	int apply(long d) throws ArithmeticException;

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue a)
			return Values.valueOf(apply(a.value()));
		return Values.INT_VALUE;
	}
}
