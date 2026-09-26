package me.darknet.assembler.backend.jvm.compile.analysis.func;

import me.darknet.assembler.backend.jvm.compile.analysis.Value;
import me.darknet.assembler.backend.jvm.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(int, int) throws Throwable} executor.
 */
public interface II2IThrowingStaticFunc extends StaticFunc {
	/**
	 * Applies the operation to typed arguments.
	 *
	 * @param a
	 * 		Typed operation argument.
	 * @param b
	 * 		Typed operation argument.
	 *
	 * @return Result of applying the operation.
	 *
	 * @throws ArithmeticException
	 * 		If the operation cannot be applied to the supplied arguments.
	 */
	int apply(int a, int b) throws ArithmeticException;

	@Override
	@Nullable
	default Value apply(@NotNull List<Value> params) {
		if (params.size() == 2 &&
				params.get(0) instanceof Value.KnownIntValue a &&
				params.get(1) instanceof Value.KnownIntValue b) {
			try {
				return Values.valueOf(apply(a.value(), b.value()));
			} catch (Throwable ignored) {
			}
		}
		return Values.INT_VALUE;
	}
}
