package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Untyped Dalvik <i>"zero"</i> value occupying a register pair. Its bits are the same for a {@code long} zero and a
 * {@code double} {@code +0.0}, so it stays untyped until a consumer reads it as one of those types.
 */
public enum DalvikWideZeroValue implements Value.BackendMarker {
	INSTANCE;

	@Override
	public @NotNull String valueAsString() {
		return "0";
	}

	@Override
	public boolean isKnown() {
		return true;
	}
}
