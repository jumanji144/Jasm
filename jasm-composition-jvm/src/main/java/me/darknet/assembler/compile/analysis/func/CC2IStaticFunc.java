package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(char, char)} executor.
 */
public interface CC2IStaticFunc extends StaticFunc {
    int apply(char a, char b);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 2 &&
                params.get(0) instanceof Value.KnownIntValue a &&
                params.get(1) instanceof Value.KnownIntValue b)
            return Values.valueOf(apply((char) a.value(), (char) b.value()));
        return Values.INT_VALUE;
    }
}
