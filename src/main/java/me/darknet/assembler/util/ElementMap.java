package me.darknet.assembler.util;

import me.darknet.assembler.ast.ASTElement;

import java.util.ArrayList;
import java.util.List;

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
	public <T extends B> T get(String content) {
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
