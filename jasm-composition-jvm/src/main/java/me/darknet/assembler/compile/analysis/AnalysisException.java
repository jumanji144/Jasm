package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.simulation.SimulationException;
import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper of possible JVM analysis failures.
 * <p/>
 * Common cases:
 * <ul>
 *   <li>{@link #getCause()} {@code instanceof} {@link FrameMergeException}</li>
 *   <li>{@link #getCause()} is any exception raised when executing an instruction in the analysis engine</li>
 *   <li>Analysis work-list exceeded capacity, short-circuited to prevent excessive computation</li>
 *   <li>Branch target is unknown, cannot correctly complete analysis</li>
 * </ul>
 */
public class AnalysisException extends SimulationException {
	public enum FailureKind {
		FRAME_MERGE,
		INVALID_CONTROL_FLOW,
		QUEUE_OVERFLOW,
		ENGINE_BUG
	}

	private final CodeElement element;
	private final FailureKind kind;

	public AnalysisException(@Nullable CodeElement element, @NotNull FailureKind kind, @Nullable Throwable cause, @NotNull String message) {
		super(message, cause);
		this.element = element;
		this.kind = kind;
	}

	public AnalysisException(@NotNull FailureKind kind, @NotNull Throwable cause, @NotNull String message) {
		this(null, kind, cause, message);
	}

	public AnalysisException(@NotNull CodeElement element, @NotNull FailureKind kind, @NotNull String message) {
		this(element, kind, null, message);
	}

	public AnalysisException(@NotNull CodeElement element, @NotNull FailureKind kind, @NotNull Throwable cause) {
		this(element, kind, cause, cause.getMessage());
	}

	public AnalysisException(@NotNull FailureKind kind, @NotNull String message) {
		this(null, kind, null, message);
	}

	/**
	 * @return Element linked to the failure. May be {@code null} in some cases.
	 */
	@Nullable
	public CodeElement getElement() {
		return element;
	}

	public @NotNull FailureKind getKind() {
		return kind;
	}
}
