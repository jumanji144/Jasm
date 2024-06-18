package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compiler.InheritanceChecker;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AnalysisUtils {
    /**
     * @param checker
     *                Inheritance checker to use for determining common super-types.
     * @param a
     *                Some type.
     * @param b
     *                Some type.
     *
     * @return Common type between the two.
     */
    @Nullable
    public static ClassType commonType(@NotNull InheritanceChecker checker, @Nullable ClassType a, @Nullable ClassType b) {
        if (a == null && b == null) return null;
        if (a != null && b == null) return a;
        if (a == null && b != null) return b;

        if (a instanceof PrimitiveType ap && b instanceof PrimitiveType bp) {
            return bp.widen(ap.widen(bp));
        } else if (a instanceof ObjectType ao && b instanceof ObjectType bo) {
            if (ao.equals(bo))
                return ao;

            String commonType = checker.getCommonSuperclass(ao.internalName(), bo.internalName());

            if (commonType == null)
                return Types.OBJECT;
            else
                return Types.objectTypeFromInternalName(commonType);
        } else {
            return Types.OBJECT;
        }
    }
}
