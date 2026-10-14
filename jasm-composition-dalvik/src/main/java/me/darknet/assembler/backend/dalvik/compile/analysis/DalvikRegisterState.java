package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Contents of a Dalvik method's register words.
 *
 * @param slots
 * 		One value per register word.
 * @param pendingResult
 * 		Implicit result awaiting a move-result instruction, or {@code null} when absent.
 */
public record DalvikRegisterState(@NotNull List<Slot> slots, @Nullable Value pendingResult) {
	/**
	 * @param register
	 * 		Register number.
	 *
	 * @return The slot stored at that register.
	 */
	public @NotNull Slot slot(int register) {
		return slots.get(register);
	}

	/**
	 * @return The number of register words in this state.
	 */
	public int registerCount() {
		return slots.size();
	}

	/**
	 * One register word in a Dalvik register state.
	 */
	public sealed interface Slot permits ValueHead, WideTail, Undefined {}

	/**
	 * Head word containing a value.
	 *
	 * @param value
	 * 		Register value.
	 */
	public record ValueHead(@NotNull Value value) implements Slot {}

	/**
	 * Tail word of a wide value.
	 *
	 * @param headRegister
	 * 		Register containing the matching {@link ValueHead}.
	 */
	public record WideTail(int headRegister) implements Slot {}

	/**
	 * Unwritten register word, distinct from a typed value that is unknown.
	 */
	public enum Undefined implements Slot {
		INSTANCE
	}
}
