package me.darknet.assembler.compile.analysis;

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
	 * @param index Key.
	 * @return Frame at index.
	 */
	Frame getFrame(int index);

	/**
	 * @param index Key.
	 * @param frameSupplier Lookup for frame to put, if index has no associated frame.
	 * @return Frame at index.
	 */
	default Frame putFrameIfAbsent(int index, @NotNull Supplier<Frame> frameSupplier) {
		Frame frame = getFrame(index);
		if (frame == null) {
			frame = frameSupplier.get();
			putFrame(index, frame);
		}
		return frame;
	}

	/**
	 * @param index Key.
	 * @param frame Frame to put.
	 */
	void putFrame(int index, Frame frame);
}
