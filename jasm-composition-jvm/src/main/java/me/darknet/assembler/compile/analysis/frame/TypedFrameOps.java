package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;
import org.jetbrains.annotations.NotNull;

public class TypedFrameOps implements FrameOps<TypedFrame> {
	@Override
	public @NotNull TypedFrame newEmptyFrame() {
		return new TypedFrameImpl();
	}

	@Override
	public void setFrameLocal(@NotNull TypedFrame frame, int idx, @NotNull Local param) {
		frame.setLocal(idx, param);
	}
}
