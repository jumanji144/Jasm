package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code long method(long, long)} executor.
 */
public interface JJ2JStaticFunc extends StaticFunc {
    long apply(long a, long b);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 2 &&
                params.get(0) instanceof Value.KnownLongValue a &&
                params.get(1) instanceof Value.KnownLongValue b)
            return Values.valueOf(apply(a.value(), b.value()));
        return Values.LONG_VALUE;
    }
}
