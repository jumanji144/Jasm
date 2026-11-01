package me.darknet.assembler.backend.dalvik.compile.visitor;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Utility for allocating Dalvik registers.
 */
final class DalvikRegisterAllocation {
	private DalvikRegisterAllocation() {}

	/**
	 * Finds the first unused slot at or after {@code start}, bounded by {@code ceiling}.
	 *
	 * @return The first free slot, or {@code ceiling} when none is available.
	 */
	static int firstFree(@NotNull Set<Integer> usedRegisters, int start, int ceiling) {
		int index = start;
		while (index < ceiling && usedRegisters.contains(index))
			index++;
		return index;
	}

	/**
	 * Finds the first pair of adjacent unused slots at or after {@code start}, bounded by {@code ceiling}.
	 *
	 * @return The first slot of the pair, or the first candidate that cannot fit when none is available.
	 */
	static int firstFreeWide(@NotNull Set<Integer> usedRegisters, int start, int ceiling) {
		int index = start;
		while ((long) index + 1 < ceiling && (usedRegisters.contains(index) || usedRegisters.contains(index + 1)))
			index++;
		return index;
	}
}
