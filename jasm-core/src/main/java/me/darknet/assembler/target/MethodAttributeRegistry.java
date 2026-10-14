package me.darknet.assembler.target;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ordered registry of target-owned method-body attribute parsers.
 */
public final class MethodAttributeRegistry {
    private final Map<String, MethodAttributeParser> parsers;
    private final boolean mutable;

    /**
     * Creates an empty mutable registry.
     */
    public MethodAttributeRegistry() {
        this(new LinkedHashMap<>(), true);
    }

    private MethodAttributeRegistry(Map<String, MethodAttributeParser> parsers, boolean mutable) {
        this.parsers = parsers;
        this.mutable = mutable;
    }

    /**
     * @return Immutable empty registry.
     */
    public static @NotNull MethodAttributeRegistry empty() {
        return new MethodAttributeRegistry(Map.of(), false);
    }

    /**
     * @param parser
     * 		Parser to register.
     *
     * @throws UnsupportedOperationException
     * 		If this registry is immutable.
     * @throws IllegalArgumentException
     * 		If another parser already owns the key.
     */
    public void register(@NotNull MethodAttributeParser parser) {
        Objects.requireNonNull(parser, "parser");
        if (!mutable)
            throw new UnsupportedOperationException("Registry is immutable");
        String key = Objects.requireNonNull(parser.key(), "parser.key");
        if (parsers.containsKey(key))
            throw new IllegalArgumentException("Duplicate method attribute: " + key);
        parsers.put(key, parser);
    }

    /**
     * @param key
     * 		Exact source key to find.
     *
     * @return Parser for {@code key}, or {@code null} when absent.
     */
    public @Nullable MethodAttributeParser get(@NotNull String key) {
        return parsers.get(Objects.requireNonNull(key, "key"));
    }

    /**
     * @return Immutable registered attribute keys in insertion order.
     */
    public @NotNull Set<String> getKeywords() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(parsers.keySet()));
    }
}