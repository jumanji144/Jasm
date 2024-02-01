package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code int method(byte, byte)} executor.
 */
public interface BB2IStaticFunc extends StaticFunc {
    int apply(byte a, byte b);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 2 &&
                params.get(0) instanceof Value.KnownIntValue a &&
                params.get(1) instanceof Value.KnownIntValue b)
            return Values.valueOf(apply((byte) a.value(), (byte) b.value()));
        return Values.INT_VALUE;
    }
}
