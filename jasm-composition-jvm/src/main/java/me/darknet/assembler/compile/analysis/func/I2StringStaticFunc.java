package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static {@code String method(int)} executor.
 */
public interface I2StringStaticFunc extends StaticFunc {
    String apply(int d);

    @Override
    @Nullable
    default Value apply(@NotNull List<Value> params) {
        if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue a) {
            String value = apply(a.value());
            if (value != null)
                return Values.valueOfString(value);
        }
        return Values.STRING_VALUE;
    }
}
