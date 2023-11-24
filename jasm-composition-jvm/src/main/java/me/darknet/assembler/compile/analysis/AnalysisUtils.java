package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

public class AnalysisUtils {
	/**
	 * Dummy type used as a placeholder for null.
	 */
	public static final ObjectType NULL = Types.instanceTypeFromDescriptor("null");

	/**
	 * @param checker
	 * 		Inheritance checker to use for determining common super-types.
	 * @param a
	 * 		Some type.
	 * @param b
	 * 		Some type.
	 *
	 * @return Common type between the two.
	 */
	@NotNull
	public static ClassType commonType(@NotNull InheritanceChecker checker, @NotNull ClassType a, @NotNull ClassType b) {
		if (a instanceof PrimitiveType ap && b instanceof PrimitiveType bp) {
			return bp.widen(ap.widen(bp));
		} else if (a instanceof ObjectType ao && b instanceof ObjectType bo) {
			if (ao.equals(bo)) return ao;

			String commonType = checker.getCommonSuperclass(ao.internalName(), bo.internalName());

			if (commonType == null) return Types.OBJECT;
			else return Types.objectTypeFromInternalName(commonType);
		} else {
			return Types.OBJECT;
		}
	}
}
