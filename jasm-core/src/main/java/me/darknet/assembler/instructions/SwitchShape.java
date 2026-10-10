package me.darknet.assembler.instructions;

import org.jetbrains.annotations.Nullable;

/**
 * Source-level shape of a switch instruction's payload.
 * <p>
 * Each shape names the payload keys it is written with,
 * so a consumer can read a switch without knowing which target declared it.
 */
public enum SwitchShape {
    /**
     * Inclusive bounds plus a positional case list. Case keys are implied by {@link #baseKey()}.
     */
    TABLE("min", "cases", "default"),

    /**
     * Explicit integer case keys plus a default label.
     */
    LOOKUP(null, null, "default"),

    /**
     * A first key plus a positional target list. Case keys are implied by {@link #baseKey()}.
     */
    PACKED("first", "targets", null),

    /**
     * Explicit integer case keys with no default label.
     */
    SPARSE(null, null, null);

    private final String baseKey;
    private final String casesKey;
    private final String defaultKey;

    SwitchShape(@Nullable String baseKey, @Nullable String casesKey, @Nullable String defaultKey) {
        this.baseKey = baseKey;
        this.casesKey = casesKey;
        this.defaultKey = defaultKey;
    }

    /**
     * @return Key holding the first case key, or {@code null} when every case key is written out.
     */
    public @Nullable String baseKey() {
        return baseKey;
    }

    /**
     * @return Key holding the positional case or target list, or {@code null} when case keys are written out.
     */
    public @Nullable String casesKey() {
        return casesKey;
    }

    /**
     * @return Key holding the default label, or {@code null} when the shape has no default.
     */
    public @Nullable String defaultKey() {
        return defaultKey;
    }
}
