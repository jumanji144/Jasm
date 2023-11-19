package me.darknet.assembler.compile.analysis;

import org.jetbrains.annotations.NotNull;

/**
 * Exception for when a frame merge fails.
 */
public class FrameMergeException extends Exception {
	private final AbstractFrame<?, ?> primary;
	private final AbstractFrame<?, ?> secondary;

	/**
	 * @param primary
	 * 		The frame targeted for merging.
	 * @param secondary
	 * 		The frame being merged into the primary.
	 * @param message
	 * 		Explanation of error.
	 */
	public FrameMergeException(@NotNull AbstractFrame<?, ?> primary, @NotNull AbstractFrame<?, ?> secondary, @NotNull String message) {
		super(message);

		this.primary = primary;
		this.secondary = secondary;
	}

	/**
	 * @return The frame targeted for merging.
	 */
	@NotNull
	public AbstractFrame<?, ?> getPrimary() {
		return primary;
	}

	/**
	 * @return The frame being merged into the primary.
	 */
	@NotNull
	public AbstractFrame<?, ?> getSecondary() {
		return secondary;
	}
}
