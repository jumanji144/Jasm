package me.darknet.assembler.analysis.registry;

import me.darknet.assembler.analysis.FieldReference;
import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lookup for known field values.
 */
public interface FieldValueLookup {
	/**
	 * @param field
	 * 		Field reference to look up.
	 * @param context
	 * 		Receiver value, or {@code null} for a static field.
	 *
	 * @return Field value, or {@code null} when unknown.
	 */
	@Nullable
	Value accept(@NotNull FieldReference field, @Nullable Value.ObjectValue context);
}
