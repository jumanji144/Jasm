package me.darknet.assembler.util;

import me.darknet.assembler.ast.ASTElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Map for AST elements, indexable by String and integer.
 *
 * @param <A>
 *            The key type.
 * @param <B>
 *            The value type.
 */
public class ElementMap<A extends ASTElement, B extends ASTElement> {

    private final List<Pair<A, B>> values = new ArrayList<>();

    public static <A extends ASTElement, B extends ASTElement> ElementMap<A, B> empty() {
        return new ElementMap<>();
    }

    public void put(A key, B value) {
        values.add(new Pair<>(key, value));
    }

    @SuppressWarnings("unchecked")
    public <T extends B> T get(int index) {
        return (T) values.get(index).second();
    }

    @SuppressWarnings("unchecked")
    public <T extends B> T get(String content) {
        for (Pair<A, B> pair : values) {
            if (pair.first().content().equals(content)) {
                return (T) pair.second();
            }
        }
        return null;
    }

    public A key(int index) {
        return values.get(index).first();
    }

    public A key(String content) {
        for (Pair<A, B> pair : values) {
            if (pair.first().content().equals(content)) {
                return pair.first();
            }
        }
        return null;
    }

    public Pair<A, B> pair(int index) {
        return values.get(index);
    }

    public Collection<Pair<A, B>> pairs() {
        return values;
    }

    public boolean containsKey(String content) {
        for (Pair<A, B> pair : values) {
            if (pair.first().content().equals(content)) {
                return true;
            }
        }
        return false;
    }

    public List<ASTElement> elements() {
        List<ASTElement> elements = new ArrayList<>();
        for (Pair<A, B> pair : values) {
            elements.add(pair.first());
            elements.add(pair.second());
        }
        return elements;
    }

    public int size() {
        return values.size();
    }

}
