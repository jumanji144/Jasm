package me.darknet.assembler.analysis.registry;

import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lookup for known method values.
 */
public interface MethodValueLookup {
	/**
	 * @param method
	 * 		Method reference to look up.
	 * @param context
	 * 		Receiver value, or {@code null} for a static method.
	 * @param parameters
	 * 		Method parameters.
	 *
	 * @return Method return value, or {@code null} when unknown.
	 */
	@Nullable
	Value accept(@NotNull MethodReference method, @Nullable Value.ObjectValue context, @NotNull List<Value> parameters);
}
