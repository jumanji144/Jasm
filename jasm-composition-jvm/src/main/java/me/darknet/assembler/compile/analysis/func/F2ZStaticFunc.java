package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code boolean method(float)} executor.
 */
public interface F2ZStaticFunc extends StaticFunc {
    boolean apply(float f);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 1 &&
                params.get(0) instanceof Value.KnownFloatValue a)
            return Values.valueOf(apply(a.value()));
        return Values.INT_VALUE;
    }
}
