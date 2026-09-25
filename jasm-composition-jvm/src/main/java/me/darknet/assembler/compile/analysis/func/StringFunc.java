package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Instance method executor on {@link String}.
 */
public interface StringFunc {
	/**
	 * Applies the operation to generic analysis values.
	 *
	 * @param value
	 * 		Generic value supplied to the operation.
	 * @param params
	 * 		Generic value supplied to the operation.
	 *
	 * @return Computed value, or {@code null} when the operation has no result.
	 */
	@Nullable
	Value apply(@NotNull String value, @NotNull List<Value> params);
}
