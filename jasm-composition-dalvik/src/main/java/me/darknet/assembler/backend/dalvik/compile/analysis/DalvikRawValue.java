package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Category-agnostic, single-word value originating from a nonzero Dalvik bit-pattern literal.
 * <p>
 * A raw literal has exact bits, but Dalvik does not encode whether those bits are an {@code int} or a {@code float}.
 * The {@link #UNKNOWN} value is used after joining different raw payloads or a raw value with the untyped zero sentinel.
 *
 * @param bits
 * 		Raw bits of the literal value.
 * @param known
 *        {@code true} if the bits are known, {@code false} if this is the {@link #UNKNOWN} sentinel.
 *
 * @see DalvikWideRawValue
 */
record DalvikRawValue(int bits, boolean known) implements Value.BackendMarker {
	static final DalvikRawValue UNKNOWN = new DalvikRawValue(0, false);

	static @NotNull DalvikRawValue of(int bits) {
		return new DalvikRawValue(bits, true);
	}

	static @NotNull DalvikRawValue join(@NotNull DalvikRawValue left, @NotNull DalvikRawValue right) {
		return left.known && right.known && left.bits == right.bits ? left : UNKNOWN;
	}

	@Override
	public @NotNull String valueAsString() {
		return known ? "#0x%08X".formatted(bits) : "raw";
	}

	@Override
	public boolean isKnown() {
		return known;
	}
}
