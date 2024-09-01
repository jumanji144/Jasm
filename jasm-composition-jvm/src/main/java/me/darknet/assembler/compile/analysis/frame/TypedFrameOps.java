package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;

import org.jetbrains.annotations.NotNull;

public class TypedFrameOps implements FrameOps<TypedFrame> {
    @Override
    public @NotNull TypedFrame newEmptyFrame() {
        return new TypedFrameImpl();
    }

    @Override
    public void setFrameLocal(@NotNull TypedFrame frame, int idx, @NotNull Local local) {
        frame.setLocal(idx, local);
    }

    @Override
    public void setFrameLocalNull(@NotNull TypedFrame frame, int idx, @NotNull Local local) {
        if (!local.isNull())
            throw new IllegalStateException("Usage of 'setFrameLocalNull' requires passing a valid 'null' local");
        frame.setLocal(idx, local);
    }
}
