package me.darknet.assembler.util;

import me.darknet.assembler.parser.Group;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtil {
	public static List<Group> add(List<? extends Group> list, Group item) {
		List<Group> newList = new ArrayList<>(list);
		newList.add(item);
		return newList;
	}
}
