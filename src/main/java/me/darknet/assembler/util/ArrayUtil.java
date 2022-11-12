package me.darknet.assembler.util;

import me.darknet.assembler.parser.Group;

import java.util.ArrayList;
import java.util.List;

public class ArrayUtil {

	public static Group[] add(Group[] a, Group b) {
		int aLen = a.length;
		Group[] c = new Group[aLen + 1];
		System.arraycopy(a, 0, c, 0, aLen);
		c[aLen] = b;
		return c;
	}

	public static List<Group> add(List<? extends Group> list, Group item) {
		List<Group> newList = new ArrayList<>(list);
		newList.add(item);
		return newList;
	}
}
