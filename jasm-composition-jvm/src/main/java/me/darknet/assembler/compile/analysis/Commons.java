package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

public class Commons {
	public static final ClassType OBJECT = Types.instanceType(Object.class);

	/**
	 * @param checker Inheritance checker to use for determining common super-types.
	 * @param a Some type.
	 * @param b Some type.
	 * @return Common type between the two.
	 */
	@NotNull
	public static ClassType commonType(@NotNull InheritanceChecker checker, @NotNull ClassType a, @NotNull ClassType b) {
		if (a instanceof PrimitiveType || b instanceof PrimitiveType) {
			if (isInteger(a) && isInteger(b)) {
				return Types.INT;
			}
			if (a == b) {
				return a;
			} else {
				return Types.VOID;
			}
		} else {
			ObjectType aObj = (ObjectType) a;
			ObjectType bObj = (ObjectType) b;

			if (aObj.equals(bObj)) return aObj;

			String commonType = checker.getCommonSuperclass(aObj.internalName(), bObj.internalName());

			if (commonType == null) return OBJECT;
			else return Types.objectTypeFromInternalName(commonType);
		}
	}

	/**
	 * @param type Some type to check.
	 * @return {@code true} when the type is within the scope of an {@code int}.
	 */
	public static boolean isInteger(ClassType type) {
		return type == Types.BYTE || type == Types.SHORT || type == Types.INT || type == Types.CHAR || type == Types.BOOLEAN;
	}
}
