package me.darknet.assembler.analysis.func;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(float)} executor.
 */
public interface F2IStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param f
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 */
	int apply(float f);

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 1 &&
				params.getFirst() instanceof Value.KnownFloatValue a)
			return Values.valueOf(apply(a.value()));
		return Values.INT_VALUE;
	}
}
