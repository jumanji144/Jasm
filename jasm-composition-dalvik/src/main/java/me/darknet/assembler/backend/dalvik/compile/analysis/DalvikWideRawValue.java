package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Category-agnostic, two-word value originating from a nonzero Dalvik wide bit-pattern literal.
 * <p>
 * A raw literal has exact bits, but Dalvik does not encode whether those bits are a {@code long} or a {@code double}.
 * The {@link #UNKNOWN} value is used after joining different raw payloads or a raw value with the untyped zero sentinel.
 *
 * @param bits
 * 		Raw bits of the literal value.
 * @param known
 *        {@code true} if the bits are known, {@code false} if this is the {@link #UNKNOWN} sentinel.
 *
 * @see DalvikRawValue
 */
record DalvikWideRawValue(long bits, boolean known) implements Value.BackendMarker {
	static final DalvikWideRawValue UNKNOWN = new DalvikWideRawValue(0L, false);

	static @NotNull DalvikWideRawValue of(long bits) {
		return new DalvikWideRawValue(bits, true);
	}

	static @NotNull DalvikWideRawValue join(@NotNull DalvikWideRawValue left, @NotNull DalvikWideRawValue right) {
		return left.known && right.known && left.bits == right.bits ? left : UNKNOWN;
	}

	@Override
	public @NotNull String valueAsString() {
		return known ? "#0x%016X".formatted(bits) : "raw";
	}

	@Override
	public boolean isKnown() {
		return known;
	}
}
