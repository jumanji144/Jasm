package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.ValuedLocal;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;

public class ValuedFrameOps implements FrameOps<ValuedFrame> {
	@Override
	public @NotNull ValuedFrame newEmptyFrame() {
		return new ValuedFrameImpl();
	}

	@Override
	public void setFrameLocal(@NotNull ValuedFrame frame, int idx, @NotNull Local param) {
		// Adapt to valued locals
		frame.setLocal(idx, new ValuedLocal(param, Values.valueOf(param.type())));
	}
}
