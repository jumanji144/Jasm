package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.AbstractFrame;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.StackEntry;
import org.jetbrains.annotations.NotNull;

public record ForkKey<L extends Local, S extends StackEntry>
		(int index, @NotNull AbstractFrame<L, S> frame) implements Comparable<ForkKey<L, S>> {
	@Override
	public int compareTo(@NotNull ForkKey other) {
		return Integer.compare(index, other.index);
	}
}
