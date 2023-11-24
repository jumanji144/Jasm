package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.VariableNameLookup;
import org.jetbrains.annotations.NotNull;

/**
 * Factory/provider of {@link JvmAnalysisEngine} instances.
 */
public interface JvmAnalysisEngineFactory {
	/**
	 * @param lookup
	 * 		Variable name lookup to use in the engine.
	 *
	 * @return New analysis engine instance.
	 */
	@NotNull JvmAnalysisEngine<?> create(@NotNull VariableNameLookup lookup);
}
