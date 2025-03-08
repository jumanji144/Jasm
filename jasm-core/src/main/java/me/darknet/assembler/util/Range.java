package me.darknet.assembler.util;

import org.jetbrains.annotations.NotNull;

public record Range(int start, int end) {
    public static final Range EMPTY = new Range(-1, -1);

    public boolean within(int pos) {
        return pos >= start && pos <= end;
    }

    public boolean withinExclusive(int pos) {
        return pos > start && pos < end;
    }

    public boolean overlap(@NotNull Range other) {
        return Math.max(start, other.start) <= Math.max(end, other.end);
    }

    public static boolean overlap(int start, int end, int otherStart, int otherEnd) {
        return Math.max(start, otherStart) <= Math.max(end, otherEnd);
    }
}
