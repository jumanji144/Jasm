package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code boolean method(int)} executor.
 */
public interface I2ZStaticFunc extends StaticFunc {
    boolean apply(int a);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue a)
            return Values.valueOf(apply((char) a.value()));
        return Values.INT_VALUE;
    }
}
