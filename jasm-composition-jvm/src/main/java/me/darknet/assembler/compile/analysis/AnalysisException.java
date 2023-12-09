package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compile.analysis.frame.FrameMergeException;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.simulation.SimulationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper of possible simulation failures.
 * <p/>
 * Common cases:
 * <ul>
 * <li>{@link #getCause()} {@code instanceof} {@link FrameMergeException}</li>
 * <li>{@link #getCause()} is any exception raised when executing an instruction
 * in the analysis engine</li>
 * <li>Analysis forking exceeded capacity, short circuited to prevent excessive
 * computation</li>
 * <li>Branch target is unknown, cannot correctly complete analysis</li>
 * </ul>
 */
public class AnalysisException extends SimulationException {
    private final CodeElement element;

    public AnalysisException(@Nullable CodeElement element, @Nullable Throwable cause, @NotNull String message) {
        super(message, cause);
        this.element = element;
    }

    public AnalysisException(@NotNull Throwable cause, @NotNull String message) {
        this(null, cause, message);
    }

    public AnalysisException(@NotNull CodeElement element, @NotNull String message) {
        this(element, null, message);
    }

    public AnalysisException(@NotNull CodeElement element, @NotNull Throwable cause) {
        this(element, cause, cause.getMessage());
    }

    public AnalysisException(@NotNull String message) {
        this(null, null, message);
    }

    /**
     * @return Element linked to the failure. May be {@code null} in some cases.
     */
    @Nullable
    public CodeElement getElement() {
        return element;
    }
}
