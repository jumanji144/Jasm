package me.darknet.assembler.util;

import org.jetbrains.annotations.NotNull;

/**
 * A range associated with a start and end position of some AST element.
 * The range is inclusive of both the start and end positions.
 *
 * @param start
 * 		The start position of the range.
 * @param end
 * 		The end position of the range.
 */
public record Range(int start, int end) implements Comparable<Range> {
	/** Empty range, used to represent a range that does not exist. */
	public static final Range EMPTY = new Range(-1, -1);

	@Override
	public int compareTo(@NotNull Range o) {
		int cmp = Integer.compare(start, o.start);
		if (cmp == 0)
			cmp = Integer.compare(end, o.end);
		return cmp;
	}

	/**
	 * @param pos
	 * 		The position to check
	 *
	 * @return {@code true} if the position is within this range, {@code false} otherwise
	 */
	public boolean within(int pos) {
		return pos >= start && pos <= end;
	}

	/**
	 * @param pos
	 * 		The position to check
	 *
	 * @return {@code true} if the position is within this range, {@code false} otherwise
	 */
	public boolean withinExclusive(int pos) {
		return pos > start && pos < end;
	}

	/**
	 * @param other
	 * 		The other range to check
	 *
	 * @return {@code true} if the other range overlaps with this range, {@code false} otherwise
	 */
	public boolean overlap(@NotNull Range other) {
		return Math.max(start, other.start) <= Math.max(end, other.end);
	}

	/**
	 * @param start
	 * 		The start of the other range
	 * @param end
	 * 		The end of the other range
	 *
	 * @return {@code true} if the other range overlaps with this range, {@code false} otherwise
	 */
	public static boolean overlap(int start, int end, int otherStart, int otherEnd) {
		return Math.max(start, otherStart) <= Math.max(end, otherEnd);
	}
}
