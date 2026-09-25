package me.darknet.assembler.backend.dalvik;

import me.darknet.assembler.helper.Handle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps method-handle kinds between the assembler's vocabulary and the dex encoding.
 */
public final class DalvikHandleKinds {
	private static final Map<Handle.Kind, Integer> DEX_KINDS = Map.ofEntries(
			Map.entry(Handle.Kind.GET_FIELD, me.darknet.dex.tree.definitions.constant.Handle.KIND_INSTANCE_GET),
			Map.entry(Handle.Kind.GET_STATIC, me.darknet.dex.tree.definitions.constant.Handle.KIND_STATIC_GET),
			Map.entry(Handle.Kind.PUT_FIELD, me.darknet.dex.tree.definitions.constant.Handle.KIND_INSTANCE_PUT),
			Map.entry(Handle.Kind.PUT_STATIC, me.darknet.dex.tree.definitions.constant.Handle.KIND_STATIC_PUT),
			Map.entry(Handle.Kind.INVOKE_VIRTUAL, me.darknet.dex.tree.definitions.constant.Handle.KIND_INVOKE_INSTANCE),
			Map.entry(Handle.Kind.INVOKE_STATIC, me.darknet.dex.tree.definitions.constant.Handle.KIND_INVOKE_STATIC),
			Map.entry(Handle.Kind.INVOKE_SPECIAL, me.darknet.dex.tree.definitions.constant.Handle.KIND_INVOKE_DIRECT),
			Map.entry(Handle.Kind.NEW_INVOKE_SPECIAL, me.darknet.dex.tree.definitions.constant.Handle.KIND_INVOKE_CONSTRUCTOR),
			Map.entry(Handle.Kind.INVOKE_INTERFACE, me.darknet.dex.tree.definitions.constant.Handle.KIND_INVOKE_INTERFACE)
	);
	private static final Map<Integer, Handle.Kind> ASSEMBLER_KINDS = invert(DEX_KINDS);

	private DalvikHandleKinds() {}

	/**
	 * @param kind
	 * 		Assembler handle kind.
	 *
	 * @return Dex method-handle kind the handle encodes as.
	 *
	 * @throws IllegalStateException
	 * 		If the kind has no dex encoding, which would mean the two vocabularies have diverged.
	 */
	public static int toDex(@NotNull Handle.Kind kind) {
		Integer dexKind = DEX_KINDS.get(kind);
		if (dexKind == null) throw new IllegalStateException("No dex method-handle kind for " + kind);
		return dexKind;
	}

	/**
	 * @param dexKind
	 * 		Dex method-handle kind read from a file.
	 *
	 * @return Assembler handle kind the dex kind encodes as.
	 *
	 * @throws IllegalStateException
	 * 		If the dex kind has no assembler encoding, which would mean the two vocabularies have diverged.
	 */
	public static @NotNull Handle.Kind fromDex(int dexKind) {
		Handle.Kind kind = ASSEMBLER_KINDS.get(dexKind);
		if (kind == null)
			throw new IllegalStateException("No assembler handle kind for dex kind 0x" + Integer.toHexString(dexKind));
		return kind;
	}

	/**
	 * @param dexKind
	 * 		Dex method-handle kind read from a file.
	 *
	 * @return Source keyword that parses back into this kind,
	 * or {@code null} when the kind is one this dialect has no spelling for.
	 */
	public static @Nullable String keyword(int dexKind) {
		Handle.Kind kind = ASSEMBLER_KINDS.get(dexKind);
		return kind == null ? null : Handle.KIND_NAMES.get(kind);
	}

	/**
	 * @param kinds
	 * 		Mapping to reverse.
	 * @param <K>
	 * 		Key type of the supplied mapping, which becomes the value type.
	 * @param <V>
	 * 		Value type of the supplied mapping, which becomes the key type.
	 *
	 * @return Mapping with keys and values exchanged.
	 */
	private static <K, V> Map<V, K> invert(@NotNull Map<K, V> kinds) {
		Map<V, K> inverted = new HashMap<>(kinds.size());
		kinds.forEach((key, value) -> inverted.put(value, key));
		return Map.copyOf(inverted);
	}
}
