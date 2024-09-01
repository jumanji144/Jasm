package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.VarCache;

import org.jetbrains.annotations.NotNull;

/**
 * Factory/provider of {@link JvmAnalysisEngine} instances.
 */
public interface JvmAnalysisEngineFactory {
    /**
     * @param varCache
     *               Variable cache for name/index lookup within the engine.
     *
     * @return New analysis engine instance.
     */
    @NotNull
    JvmAnalysisEngine<?> create(@NotNull VarCache varCache);
}
