package me.darknet.assembler.util;

import me.darknet.assembler.parser.Group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupLists {
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

	@SuppressWarnings("all")
	public static List<Group> fromArray(Object... items) {
		List<Group> newList = new ArrayList<>();
		for (Object item : items) {
			if (item instanceof Group) {
				newList.add((Group) item);
			} else if (item instanceof List) {
				newList.addAll((List<Group>) item);
			}
		}
		return newList;
	}
}
