package me.darknet.assembler.util;

public class ArrayUtil {

    public static <T> T[] combine(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) new Object[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public static <T> T[] add(T[] a, T b) {
        int aLen = a.length;
        @SuppressWarnings("unchecked")
        T[] c = (T[]) new Object[aLen + 1];
        System.arraycopy(a, 0, c, 0, aLen);
        c[aLen] = b;
        return c;
    }

}
