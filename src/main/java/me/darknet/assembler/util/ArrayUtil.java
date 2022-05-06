package me.darknet.assembler.util;

import me.darknet.assembler.parser.Group;

public class ArrayUtil {

    public static Group[] add(Group[] a, Group b) {
        int aLen = a.length;
        @SuppressWarnings("unchecked")
        Group[] c = new Group[aLen + 1];
        System.arraycopy(a, 0, c, 0, aLen);
        c[aLen] = b;
        return c;
    }

}
