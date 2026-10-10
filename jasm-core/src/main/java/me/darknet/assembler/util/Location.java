package me.darknet.assembler.util;

import org.jetbrains.annotations.NotNull;

/**
 * Source location identified by line, column, length, and source name.
 *
 * @param line
 * 		One-based line containing the location.
 * @param column
 * 		One-based column containing the location.
 * @param length
 * 		Length of the source span at the location.
 * @param source
 * 		Source name, or {@code null} when no source name is available.
 */
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
