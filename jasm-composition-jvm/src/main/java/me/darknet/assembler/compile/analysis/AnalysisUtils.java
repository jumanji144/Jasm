package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public class AnalysisUtils {
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
	@Nullable
	public static Type commonType(@NotNull InheritanceChecker checker, @Nullable Type a, @Nullable Type b) {
		return JvmTypeUtils.commonType(checker, a, b);
	}
}
