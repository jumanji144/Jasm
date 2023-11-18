package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Trackable frame state for analysis purposes.
 */
public interface FrameState {
	/**
	 * @return Last frame.
	 */
	Frame getLastFrame();

	/**
	 * @param index
	 * 		Key.
	 *
	 * @return Frame at index.
	 */
	Frame getFrame(int index);

	/**
	 * Put and merge a given frame into the given index.
	 *
	 * @param index
	 * 		Key.
	 * @param frame
	 * 		Frame to put at the index.
	 * 		Will be merged into the existing frame at the index if a frame exists at the index.
	 * @param checker
	 * 		Inheritance checker for frame merging.
	 *
	 * @return The frame at the index. Will be {@code frame} if no prior frame existed at the index.
	 * Will be the existing frame, after merging with {@code frame} if it existed previously at the index.
	 */
	default boolean mergeInto(int index, @NotNull Frame frame, @NotNull InheritanceChecker checker) {
		// If there is no existing frame, there is no merge to be done.
		Frame existing = getFrame(index);
		if (existing == null) {
			putFrame(index, frame);
			return false;
		}

		Frame mergeTarget = existing.copy();
		boolean changed = mergeTarget.merge(checker, frame);
		if (changed) putFrame(index, mergeTarget);
		return changed;
	}

	/**
	 * @param index
	 * 		Key.
	 * @param frame
	 * 		Frame to put.
	 */
	void putFrame(int index, Frame frame);
}
