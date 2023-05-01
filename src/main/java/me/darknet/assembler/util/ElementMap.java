package me.darknet.assembler.util;

import me.darknet.assembler.ast.ASTElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Map for AST elements, indexable by String and integer.
 * @param <A> The key type.
 * @param <B> The value type.
 */
public class ElementMap<A extends ASTElement, B extends ASTElement> {

	public static <A extends ASTElement, B extends ASTElement> ElementMap<A, B> empty() {
		return new ElementMap<>();
	}

	private final List<Pair<A, B>> values = new ArrayList<>();

	public void put(A key, B value) {
		values.add(new Pair<>(key, value));
	}

	@SuppressWarnings("unchecked")
	public <T extends B> T get(int index) {
		return (T) values.get(index).getSecond();
	}

	@SuppressWarnings("unchecked")
	public <T extends B> @Nullable T get(String content) {
		for (Pair<A, B> pair : values) {
			if (pair.getFirst().getContent().equals(content)) {
				return (T) pair.getSecond();
			}
		}
		return null;
	}

	public A getKey(int index) {
		return values.get(index).getFirst();
	}

	public A getKey(String content) {
		for (Pair<A, B> pair : values) {
			if (pair.getFirst().getContent().equals(content)) {
				return pair.getFirst();
			}
		}
		return null;
	}

	public Pair<A, B> getPair(int index) {
		return values.get(index);
	}

	public Collection<Pair<A, B>> getPairs() {
		return values;
	}

	public boolean containsKey(String content) {
		for (Pair<A, B> pair : values) {
			if (pair.getFirst().getContent().equals(content)) {
				return true;
			}
		}
		return false;
	}

	public List<ASTElement> getElements() {
		List<ASTElement> elements = new ArrayList<>();
		for (Pair<A, B> pair : values) {
			elements.add(pair.getFirst());
			elements.add(pair.getSecond());
		}
		return elements;
	}

	public int size() {
		return values.size();
	}

}
