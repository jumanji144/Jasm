package me.darknet.assembler.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CollectionUtil {

    public static <T> List<T> fromArray(@Nullable T[] array) {
        return Arrays.asList(array);
    }

    public static <T> List<T> merge(final Collection<? extends @Nullable T> a,
            final Collection<? extends @Nullable T> b) {
        if (a == null) {
            return new ArrayList<>(b);
        }
        if (b == null) {
            return new ArrayList<>(a);
        }
        List<T> list = new ArrayList<>(a);
        list.addAll(b);
        return list;
    }

    @SafeVarargs
    public static <T> List<T> merge(final Collection<? extends @Nullable T> a, final @Nullable T... b) {
        if (a == null) {
            return fromArray(b);
        }
        if (b == null) {
            return new ArrayList<>(a);
        }
        List<T> list = new ArrayList<>(a);
        list.addAll(fromArray(b));
        return list;
    }

    public static <T> List<T> filter(@NotNull List<T> elements, Class<T> type) {
        List<T> list = new ArrayList<>();
        for (T element : elements) {
            if (type.isInstance(element)) {
                list.add(element);
            }
        }
        return list;
    }

    public static <T> @Nullable T get(List<T> T, int index) {
        if (index < 0 || index >= T.size()) {
            return null;
        }
        return T.get(index);
    }
}
