package me.darknet.assembler.compile.analysis.func;

import me.darknet.assembler.compile.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static method executor.
 */
public interface StaticFunc {
    @Nullable
    Value apply(@NotNull List<Value> params);
}
