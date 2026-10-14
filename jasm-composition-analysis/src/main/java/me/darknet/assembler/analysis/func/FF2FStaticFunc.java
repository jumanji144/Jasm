package me.darknet.assembler.analysis.func;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code float method(float, float)} executor.
 */
public interface FF2FStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param a
	 * 		Typed operation argument.
	 * @param b
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 */
	float apply(float a, float b);

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 2 &&
				params.get(0) instanceof Value.KnownFloatValue a &&
				params.get(1) instanceof Value.KnownFloatValue b)
			return Values.valueOf(apply(a.value(), b.value()));
		return Values.FLOAT_VALUE;
	}
}
