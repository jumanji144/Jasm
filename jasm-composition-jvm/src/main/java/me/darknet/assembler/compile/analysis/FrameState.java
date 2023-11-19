package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

/**
 * Trackable frame state for analysis purposes.
 */
public interface FrameState<L extends Local, S extends StackEntry, F extends AbstractFrame<L,S>> {
	/**
	 * @param index
	 * 		Key.
	 *
	 * @return Frame at index.
	 */
	F getFrame(int index);

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
	 * @return {@code true} when the merge operation at the given index resulted in a change,
	 * or there was no frame at the given index.
	 *
	 * @throws FrameMergeException
	 * 		When frame merging fails. See {@link AbstractFrame#merge(InheritanceChecker, AbstractFrame)}.
	 */
	@SuppressWarnings("unchecked")
	default boolean mergeInto(int index, @NotNull F frame, @NotNull InheritanceChecker checker) throws FrameMergeException {
		// If there is no existing frame, there is no merge to be done.
		F existing = getFrame(index);
		if (existing == null) {
			putFrame(index, frame);
			return true;
		}

		F mergeTarget = (F) existing.copy();
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
	void putFrame(int index, F frame);
}
