package me.darknet.assembler.backend.jvm.compile.analysis.frame;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.backend.jvm.compile.analysis.Local;
import me.darknet.assembler.backend.jvm.compile.analysis.ValuedLocal;
import me.darknet.assembler.backend.jvm.util.JvmTypeUtils;
import me.darknet.assembler.descriptor.ClassDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

/**
 * Frame operations for {@link ValuedFrame}.
 */
public class ValuedFrameOps implements FrameOps<ValuedFrame> {
	@Override
	public @NotNull ValuedFrame newEmptyFrame() {
		return new ValuedFrameImpl();
	}

	@Override
	public void setFrameLocal(@NotNull ValuedFrame frame, int idx, @NotNull Local local) {
		// Adapt to valued locals
		frame.setLocal(idx, new ValuedLocal(local, valueOfType(local.safeType())));
	}

	@Override
	public void setFrameLocalNull(@NotNull ValuedFrame frame, int idx, @NotNull Local local) {
		if (!local.isNull())
			throw new IllegalStateException("Usage of 'setFrameLocalNull' requires passing a valid 'null' local");
		frame.setLocal(idx, new ValuedLocal(local, Values.NULL_VALUE));
	}

	/**
	 * @param type
	 * 		Type to convert.
	 *
	 * @return Valued frame value corresponding to the given type,
	 * or {@link Values#NULL_VALUE} for null types,
	 * or {@link Values#TOP_VALUE} for top types.
	 */
	public static @NotNull Value valueOfType(@Nullable Type type) {
		if (type == null || JvmTypeUtils.isNullMarker(type))
			return Values.NULL_VALUE;
		if (JvmTypeUtils.isTop(type))
			return Values.TOP_VALUE;
		if (JvmTypeUtils.isUninitialized(type)) {
			ClassDescriptor desc = (ClassDescriptor) JvmTypeUtils.toDescriptorType(JvmTypeUtils.uninitializedOwner(type));
			return new Value.UninitializedReferenceValue(
					desc,
					JvmTypeUtils.uninitializedIdentity(type));
		}
		return Values.valueOf(JvmTypeUtils.toDescriptorType(type));
	}
}
