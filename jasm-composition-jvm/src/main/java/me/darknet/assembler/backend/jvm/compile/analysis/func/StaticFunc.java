package me.darknet.assembler.backend.jvm.compile.analysis.func;

import me.darknet.assembler.backend.jvm.compile.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static method executor.
 */
public interface StaticFunc {
	/**
	 * Applies the operation to generic analysis values.
	 *
	 * @param params
	 * 		Generic value supplied to the operation.
	 *
	 * @return Computed value, or {@code null} when the operation has no result.
	 */
	@Nullable
	Value apply(@NotNull List<Value> params);
}
