package me.darknet.assembler.backend.jvm.compile.analysis.func;

import me.darknet.assembler.backend.jvm.compile.analysis.Value;
import me.darknet.assembler.backend.jvm.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code float method(int)} executor.
 */
public interface I2FStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param i
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 */
	float apply(int i);

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue a)
			return Values.valueOf(apply(a.value()));
		return Values.FLOAT_VALUE;
	}
}
