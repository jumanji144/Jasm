package me.darknet.assembler.util;

public record Range(int start, int end) {
	public boolean within(int pos) {
		return pos >= start && pos <= end;
	}

	public boolean withinExclusive(int pos) {
		return pos > start && pos < end;
	}
}
