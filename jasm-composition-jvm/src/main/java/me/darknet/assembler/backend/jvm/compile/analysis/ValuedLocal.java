package me.darknet.assembler.backend.jvm.compile.analysis;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.ValueMergeException;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.backend.jvm.compile.analysis.frame.ValuedFrameOps;
import me.darknet.assembler.backend.jvm.compile.analysis.jvm.JvmValueMerger;
import me.darknet.assembler.backend.jvm.util.JvmTypeUtils;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.descriptor.DescriptorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.Objects;

/**
 * A local variable description paired with the value tracked by JVM analysis.
 */
public class ValuedLocal extends Local {
	private final Value value;

	public ValuedLocal(@NotNull Local other, @NotNull Value value) {
		this(other.index(), other.name(), other.type(), value);
	}

	public ValuedLocal(int index, @NotNull String name, @NotNull Value value) {
		this(index, name, typeOf(value), value);
	}

	public ValuedLocal(int index, @NotNull String name, @Nullable Type type, @NotNull Value value) {
		super(index, name, type);
		this.value = value;
	}

	public static @Nullable Type typeOf(@NotNull Value value) {
		if (value instanceof Value.TopValue)
			return JvmTypeUtils.TOP;
		if (value instanceof Value.BackendMarker)
			return null;
		DescriptorType type = value.type();
		return type == null ? null : JvmTypeUtils.toAsmType(type);
	}

	@NotNull
	public ValuedLocal asNull() {
		if (isNull()) return this;
		return new ValuedLocal(index, name, null, value);
	}

	@NotNull
	public ValuedLocal adaptType(@NotNull Type newType) {
		if (Objects.equals(type, newType))
			return this;
		return new ValuedLocal(index, name, newType, ValuedFrameOps.valueOfType(newType));
	}

	@NotNull
	public ValuedLocal mergeWith(@NotNull InheritanceChecker checker, @NotNull ValuedLocal other) throws ValueMergeException {
		if (value instanceof Value.TopValue || other.value instanceof Value.TopValue)
			return new ValuedLocal(index, name, Values.TOP_VALUE);
		if (isNull() && !other.isNull())
			return other;
		if (!isNull() && other.isNull())
			return this;
		Value merged = JvmValueMerger.merge(checker, value, other.value);
		return new ValuedLocal(index, name, typeOf(merged), merged);
	}

	@NotNull
	public Value value() {return value;}

	@Override
	public boolean equals(Object other) {
		if (!super.equals(other))
			return false;
		return value.equals(((ValuedLocal) other).value);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + value.hashCode();
	}

	@Override
	public String toString() {
		return "ValuedLocal{" + "index=" + index + "'" + ", name='" + name + '\''
				+ (isNull() ? ", null=true" : ", value=" + value) + '}';
	}
}
