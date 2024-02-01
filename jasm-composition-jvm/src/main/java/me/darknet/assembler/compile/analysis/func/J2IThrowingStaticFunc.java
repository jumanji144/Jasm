package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(long) throws Throwable} executor.
 */
public interface J2IThrowingStaticFunc extends StaticFunc {
    int apply(long d) throws ArithmeticException;

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 1 && params.get(0) instanceof Value.KnownLongValue a) {
            try {
                return Values.valueOf(apply(a.value()));
            } catch (Throwable ignored) {
            }
        }
        return Values.INT_VALUE;
    }
}
