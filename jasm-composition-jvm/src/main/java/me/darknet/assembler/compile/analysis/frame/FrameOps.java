package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;
import org.jetbrains.annotations.NotNull;

public interface FrameOps<F extends Frame> {
    /**
     * @return New empty frame.
     */
    @NotNull
    F newEmptyFrame();

    /**
     * @param frame
     *         Frame to set local value within.
     * @param idx
     *         Local variable index.
     * @param local
     *         Local variable state.
     */
    void setFrameLocal(@NotNull F frame, int idx, @NotNull Local local);

    /**
     * @param frame
     *         Frame to set local value within.
     * @param idx
     *         Local variable index representing {@code null}.
     * @param local
     *         Local variable state.
     */
    void setFrameLocalNull(@NotNull F frame, int idx, @NotNull Local local);
}
