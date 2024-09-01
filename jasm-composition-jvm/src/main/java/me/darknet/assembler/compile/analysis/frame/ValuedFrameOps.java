package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValuedLocal;
import me.darknet.assembler.compile.analysis.Values;

import org.jetbrains.annotations.NotNull;

public class ValuedFrameOps implements FrameOps<ValuedFrame> {
    @Override
    public @NotNull ValuedFrame newEmptyFrame() {
        return new ValuedFrameImpl();
    }

    @Override
    public void setFrameLocal(@NotNull ValuedFrame frame, int idx, @NotNull Local local) {
        // Adapt to valued locals
        frame.setLocal(idx, new ValuedLocal(local, Values.valueOf(local.safeType())));
    }

    @Override
    public void setFrameLocalNull(@NotNull ValuedFrame frame, int idx, @NotNull Local local) {
        if (!local.isNull())
            throw new IllegalStateException("Usage of 'setFrameLocalNull' requires passing a valid 'null' local");
        frame.setLocal(idx, new ValuedLocal(local, Values.NULL_VALUE));
    }
}
