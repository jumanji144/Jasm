package me.darknet.assembler.processing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable class-keyed storage for target-owned semantic method extensions.
 */
public final class MethodExtensions {
	private final Map<Class<? extends MethodTargetData>, MethodTargetData> values;

	/**
	 * @param extensions
	 * 		Extensions to store.
	 */
	public MethodExtensions(@NotNull Collection<? extends MethodTargetData> extensions) {
		Objects.requireNonNull(extensions, "extensions");
		Map<Class<? extends MethodTargetData>, MethodTargetData> copy = new LinkedHashMap<>();
		for (MethodTargetData extension : extensions) {
			Objects.requireNonNull(extension, "extension");
			Class<? extends MethodTargetData> type = extension.getClass();
			copy.put(type, extension);
		}
		values = Collections.unmodifiableMap(copy);
	}

	/**
	 * @return Immutable empty extensions.
	 */
	public static @NotNull MethodExtensions empty() {
		return new MethodExtensions(List.of());
	}

	/**
	 * @param type
	 * 		Extension type to retrieve.
	 * @param <T>
	 * 		Extension type.
	 *
	 * @return Stored extension of {@code type}, or {@code null} when absent.
	 */
	public <T extends MethodTargetData> @Nullable T get(@NotNull Class<T> type) {
		Objects.requireNonNull(type, "type");
		return type.cast(values.get(type));
	}

	/**
	 * @return {@code true} when no extensions are stored.
	 */
	public boolean isEmpty() {
		return values.isEmpty();
	}

	/**
	 * @return Immutable snapshot of stored extensions in insertion order.
	 */
	public @NotNull Collection<MethodTargetData> values() {
		return List.copyOf(values.values());
	}
}
