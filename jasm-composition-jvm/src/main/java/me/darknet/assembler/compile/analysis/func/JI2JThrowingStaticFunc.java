package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code long method(long, int) throws Throwable} executor.
 */
public interface JI2JThrowingStaticFunc extends StaticFunc {
    long apply(long a, int b) throws ArithmeticException;

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 2 &&
                params.get(0) instanceof Value.KnownLongValue a &&
                params.get(1) instanceof Value.KnownIntValue b) {
            try {
                return Values.valueOf(apply(a.value(), b.value()));
            } catch (Throwable ignored) {
            }
        }
        return Values.LONG_VALUE;
    }
}
