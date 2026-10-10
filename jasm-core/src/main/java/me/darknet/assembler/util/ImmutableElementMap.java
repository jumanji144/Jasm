package me.darknet.assembler.util;

import me.darknet.assembler.ast.ASTElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Immutable ordered AST key/value pairs for live AST nodes.
 *
 * @param <A>
 *            The key type.
 * @param <B>
 *            The value type.
 */
public final class ImmutableElementMap<A extends ASTElement, B extends ASTElement> implements ElementMapView<A, B> {
    private final List<Pair<A, B>> values;

    private ImmutableElementMap(List<Pair<A, B>> values) {
        this.values = Collections.unmodifiableList(values);
    }

    public static <A extends ASTElement, B extends ASTElement> ImmutableElementMap<A, B> empty() {
        return new ImmutableElementMap<>(Collections.emptyList());
    }

    public static <A extends ASTElement, B extends ASTElement> ImmutableElementMap<A, B> copyOf(
            ElementMapView<A, B> values) {
        if (values.size() == 0) {
            return empty();
        }
        return copyOf(values.pairs());
    }

    public static <A extends ASTElement, B extends ASTElement> ImmutableElementMap<A, B> copyOf(
            Collection<? extends Pair<A, B>> values) {
        return new ImmutableElementMap<>(new ArrayList<>(values));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends B> @Nullable T get(int index) {
        if (index < 0 || index >= values.size()) {
            return null;
        }
        return (T) values.get(index).second();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends B> @Nullable T get(String content) {
        for (Pair<A, B> pair : values) {
            if (pair.first().content().equals(content)) {
                return (T) pair.second();
            }
        }
        return null;
    }

    @Override
    public @Nullable A key(int index) {
        if (index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index).first();
    }

    @Override
    public @Nullable A key(String content) {
        for (Pair<A, B> pair : values) {
            if (pair.first().content().equals(content)) {
                return pair.first();
            }
        }
        return null;
    }

    @Override
    public @Nullable Pair<A, B> pair(int index) {
        if (index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    @Override
    public Collection<Pair<A, B>> pairs() {
        return values;
    }

    @Override
    public boolean containsKey(String content) {
        return key(content) != null;
    }

    @Override
    public List<ASTElement> elements() {
        List<ASTElement> elements = new ArrayList<>(values.size() * 2);
        for (Pair<A, B> pair : values) {
            elements.add(pair.first());
            elements.add(pair.second());
        }
        return Collections.unmodifiableList(elements);
    }

    @Override
    public int size() {
        return values.size();
    }
}
