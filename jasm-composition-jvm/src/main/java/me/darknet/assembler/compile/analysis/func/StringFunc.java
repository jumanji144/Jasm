package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Instance method executor on {@link String}.
 */
public interface StringFunc {
    @Nullable
    Value apply(@NotNull String value, @NotNull List<Value> params);
}
