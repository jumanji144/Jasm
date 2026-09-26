package me.darknet.assembler.backend.jvm.compile.analysis.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Ordered worklist for control-flow exploration.
 */
public final class AnalysisWorklist {
	private final NavigableSet<Entry> entries = new TreeSet<>();

	/**
	 * Adds an instruction index to the worklist with default priority.
	 *
	 * @param index
	 * 		Instruction index to visit.
	 */
	public void add(int index) {
		add(index, 0);
	}

	/**
	 * Adds an instruction index to the worklist with a given priority.
	 *
	 * @param index
	 * 		Instruction index to visit.
	 * @param priority
	 * 		Priority associated with visiting the instruction.
	 */
	public void add(int index, int priority) {
		entries.remove(new Entry(index, priority));
		entries.add(new Entry(index, priority));
	}

	/**
	 * @return Number of entries in the worklist.
	 */
	public int size() {
		return entries.size();
	}

	/**
	 * @return Whether the worklist is empty.
	 */
	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/**
	 * Removes and returns the next entry from the worklist, or null if empty.
	 *
	 * @return Next entry to visit, or null if empty.
	 */
	public @Nullable Entry next() {
		if (entries.isEmpty())
			return null;
		return entries.pollLast();
	}

	/**
	 * Worklist entry identified by an instruction index and its visit priority.
	 *
	 * @param index
	 * 		Instruction index to visit.
	 * @param priority
	 * 		Priority associated with visiting the instruction.
	 */
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
