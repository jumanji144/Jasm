package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code double method(double, double)} executor.
 */
public interface DD2DStaticFunc extends StaticFunc {
    double apply(double a, double b);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 2 &&
                params.get(0) instanceof Value.KnownDoubleValue a &&
                params.get(1) instanceof Value.KnownDoubleValue b)
            return Values.valueOf(apply(a.value(), b.value()));
        return Values.DOUBLE_VALUE;
    }
}
