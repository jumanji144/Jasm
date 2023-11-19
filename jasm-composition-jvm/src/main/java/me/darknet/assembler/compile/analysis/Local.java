package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.PrimitiveKind;
import dev.xdark.blw.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

/**
 * Outline of a local variable.
 */
public interface Local {
	/**
	 * @return Index of this variable.
	 */
	int index();

	/**
	 * @return Display name of this variable.
	 */
	@NotNull
	String name();

	/**
	 * @return Type of this variable.
	 */
	@NotNull
	ClassType type();

	/**
	 * @param newType
	 * 		New type of variable to produce.
	 *
	 * @return Copy of this instance with the new type.
	 */
	@NotNull
	@Deprecated
	Local adaptType(@NotNull ClassType newType);

	/**
	 * @return Type size of this variable.
	 * {@code 1} for all types except for {@link double}/{@code long} which is {@code 2}.
	 */
	default int size() {
		if (type() instanceof PrimitiveType primitiveType) {
			int kind = primitiveType.kind();
			return (kind == PrimitiveKind.T_LONG || kind == PrimitiveKind.T_DOUBLE) ? 2 : 1;
		}
		return 1;
	}
}
