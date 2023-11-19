package me.darknet.assembler.compile.analysis;

import org.jetbrains.annotations.NotNull;

/**
 * Exception for when a frame merge fails.
 */
public class FrameMergeException extends Exception {
	private final Frame primary;
	private final Frame secondary;

	/**
	 * @param primary
	 * 		The frame targeted for merging.
	 * @param secondary
	 * 		The frame being merged into the primary.
	 * @param message
	 * 		Explanation of error.
	 */
	public FrameMergeException(@NotNull Frame primary, @NotNull Frame secondary, @NotNull String message) {
		super(message);

		this.primary = primary;
		this.secondary = secondary;
	}

	/**
	 * @return The frame targeted for merging.
	 */
	@NotNull
	public Frame getPrimary() {
		return primary;
	}

	/**
	 * @return The frame being merged into the primary.
	 */
	@NotNull
	public Frame getSecondary() {
		return secondary;
	}
}
