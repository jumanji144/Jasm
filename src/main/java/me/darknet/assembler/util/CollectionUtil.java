package me.darknet.assembler.util;

import me.darknet.assembler.parser.Group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionUtil {
	public static List<Group> add(List<? extends Group> list, Group item) {
		List<Group> newList = new ArrayList<>(list);
		newList.add(item);
		return newList;
	}

	@SafeVarargs
	public static List<Group> add(List<? extends Group> list, List<? extends Group>... items) {
		List<Group> newList = new ArrayList<>(list);
		for (List<? extends Group> item : items) {
			newList.addAll(item);
		}
		return newList;
	}

	public static List<Group> add(List<Group> list, Group... items) {
		List<Group> newList = new ArrayList<>(list);
		newList.addAll(Arrays.asList(items));
		return newList;
	}
}
