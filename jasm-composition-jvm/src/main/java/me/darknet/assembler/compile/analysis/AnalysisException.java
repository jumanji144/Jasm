package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;

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
public class AnalysisException extends RuntimeException {
	public enum FailureKind {
		FRAME_MERGE,
		INVALID_CONTROL_FLOW,
		QUEUE_OVERFLOW,
		ENGINE_BUG
	}

	private final AbstractInsnNode instruction;
	private final FailureKind kind;

	public AnalysisException(@Nullable AbstractInsnNode instruction, @NotNull FailureKind kind, @Nullable Throwable cause, @NotNull String message) {
		super(message, cause);
		this.instruction = instruction;
		this.kind = kind;
	}

	public AnalysisException(@NotNull AbstractInsnNode instruction, @NotNull FailureKind kind, @NotNull String message) {
		this(instruction, kind, null, message);
	}

	public AnalysisException(@NotNull AbstractInsnNode instruction, @NotNull FailureKind kind, @NotNull Throwable cause) {
		this(instruction, kind, cause, cause.getMessage());
	}

	public AnalysisException(@NotNull FailureKind kind, @NotNull String message) {
		this(null, kind, null, message);
	}

	/**
	 * @return Element linked to the failure. May be {@code null} in some cases.
	 */
	@Nullable
	public AbstractInsnNode getInstruction() {
		return instruction;
	}

	public @NotNull FailureKind getKind() {
		return kind;
	}
}
