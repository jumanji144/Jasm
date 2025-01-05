package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code double method(double)} executor.
 */
public interface D2DStaticFunc extends StaticFunc {
    double apply(double d);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 1 && params.getFirst() instanceof Value.KnownDoubleValue a)
            return Values.valueOf(apply(a.value()));
        return Values.DOUBLE_VALUE;
    }
}
