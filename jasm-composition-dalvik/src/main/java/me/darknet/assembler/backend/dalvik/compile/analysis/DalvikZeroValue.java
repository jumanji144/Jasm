package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Untyped Dalvik <i>"zero"</i> value which is used for both integers and {@code null} references.
 */
public enum DalvikZeroValue implements Value.BackendMarker {
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
