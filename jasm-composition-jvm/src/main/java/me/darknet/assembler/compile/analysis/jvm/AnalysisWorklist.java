package me.darknet.assembler.compile.analysis.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Ordered worklist for control-flow exploration.
 */
public final class AnalysisWorklist {
    private final NavigableSet<Entry> entries = new TreeSet<>();

    public void add(int index) {
        add(index, 0);
    }

    public void add(int index, int priority) {
        entries.remove(new Entry(index, priority));
        entries.add(new Entry(index, priority));
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public @Nullable Entry next() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.pollLast();
    }

    public record Entry(int index, int priority) implements Comparable<Entry> {
        @Override
        public int compareTo(@NotNull Entry other) {
            int byIndex = Integer.compare(index, other.index);
            if (byIndex != 0) {
                return byIndex;
            }
            return Integer.compare(priority, other.priority);
        }
    }
}
