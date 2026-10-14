package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A failure encountered during Dalvik method analysis.
 *
 * @param kind
 * 		Failure category.
 * @param instructionIndex
 * 		Code index associated with the failure, if known.
 * @param message
 * 		Human-readable failure detail.
 * @param sourceLocation
 * 		Original source location, if available.
 */
public record DalvikAnalysisFailure(@NotNull DalvikAnalysisFailure.FailureKind kind,
                                    @Nullable Integer instructionIndex,
                                    @NotNull String message,
                                    @Nullable Location sourceLocation) {
	/**
	 * Category of failure reported by a Dalvik analysis run.
	 */
	public enum FailureKind {
		UNSUPPORTED_TRANSFER,
		INCOMPATIBLE_MERGE,
		INTERNAL_FAILURE
	}
}
