package me.darknet.assembler.util;

import org.jetbrains.annotations.NotNull;

public record Location(int line, int column, int length, String source) implements Comparable<Location> {

    public static final Location UNKNOWN = new Location(-1, -1, -1, null);

    @Override
    public String toString() {
        if (source == null)
            return line + ":" + column;
        return source + ":" + line + ":" + column;
    }

    @Override
    public int compareTo(@NotNull Location o) {
        int cmp = Integer.compare(line, o.line);
        if (cmp == 0)
            cmp = Integer.compare(column, o.column);
        return cmp;
    }
}
