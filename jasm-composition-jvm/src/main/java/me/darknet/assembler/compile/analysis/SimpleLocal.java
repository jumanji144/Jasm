package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.PrimitiveKind;
import dev.xdark.blw.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

public record SimpleLocal(int index, String name, ClassType type) implements Local {
	@NotNull
	public SimpleLocal adaptType(@NotNull ClassType newType) {
		return new SimpleLocal(index, name, type);
	}

	/**
	 * @return Type size of this variable.
	 * {@code 1} for all types except for {@link double}/{@code long} which is {@code 2}.
	 */
	public int size() {
		if (type instanceof PrimitiveType primitiveType) {
			int kind = primitiveType.kind();
			return (kind == PrimitiveKind.T_LONG || kind == PrimitiveKind.T_DOUBLE) ? 2 : 1;
		}
		return 1;
	}
}
