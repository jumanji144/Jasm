package me.darknet.assembler.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CollectionUtil {

	public static <T> List<T> fromArray(T[] array) {
		return Arrays.asList(array);
	}

	public static <T> List<T> merge(Collection<? extends T> a, Collection<? extends T> b) {
		List<T> list = new ArrayList<>(a);
		list.addAll(b);
		return list;
	}

	@SafeVarargs
	public static <T> List<T> merge(Collection<? extends T> a, T... b) {
		List<T> list = new ArrayList<>(a);
		list.addAll(fromArray(b));
		return list;
	}

}
