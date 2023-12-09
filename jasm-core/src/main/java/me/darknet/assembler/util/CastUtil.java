package me.darknet.assembler.util;

public class CastUtil {
    @SuppressWarnings("unchecked")
    public static <T, X> T cast(X x) {
        return (T) x;
    }
}
