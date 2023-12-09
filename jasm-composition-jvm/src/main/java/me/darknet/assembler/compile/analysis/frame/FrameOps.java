package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;
import org.jetbrains.annotations.NotNull;

public interface FrameOps<F extends Frame> {
    @NotNull
    F newEmptyFrame();

    void setFrameLocal(@NotNull F frame, int idx, @NotNull Local param);
}
