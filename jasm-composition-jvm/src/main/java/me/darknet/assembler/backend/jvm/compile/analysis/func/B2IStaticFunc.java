package me.darknet.assembler.backend.jvm.compile.analysis.func;

import me.darknet.assembler.backend.jvm.compile.analysis.Value;
import me.darknet.assembler.backend.jvm.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(byte)} executor.
 */
public interface B2IStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param a
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 */
	int apply(byte a);

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue a)
			return Values.valueOf(apply((byte) a.value()));
		return Values.INT_VALUE;
	}
}
