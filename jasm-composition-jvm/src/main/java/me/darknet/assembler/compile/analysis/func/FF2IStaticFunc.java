package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(float, float)} executor.
 */
public interface FF2IStaticFunc extends StaticFunc {
    int apply(float a, float b);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 2 &&
                params.get(0) instanceof Value.KnownFloatValue a &&
                params.get(1) instanceof Value.KnownFloatValue b)
            return Values.valueOf(apply(a.value(), b.value()));
        return Values.INT_VALUE;
    }
}
