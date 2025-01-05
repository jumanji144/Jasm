package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(float)} executor.
 */
public interface F2IStaticFunc extends StaticFunc {
    int apply(float f);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 1 &&
                params.getFirst() instanceof Value.KnownFloatValue a)
            return Values.valueOf(apply(a.value()));
        return Values.INT_VALUE;
    }
}
