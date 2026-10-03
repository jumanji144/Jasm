package me.darknet.assembler.backend.dalvik.compile;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikMethodAnalysisLookup;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterUsageLookup;
import me.darknet.assembler.compiler.ClassResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dalvik class compilation result.
 *
 * @param representation
 * 		Target representation, or {@code null} when compilation failed.
 * @param analysisLookup
 * 		Results for methods emitted from the current source.
 * @param registerUsageLookup
 * 		Register access summaries keyed by method name and descriptor.
 */
public record DalvikCompileResult(@Nullable DalvikClassRepresentation representation,
                                  @NotNull DalvikMethodAnalysisLookup analysisLookup,
                                  @NotNull DalvikRegisterUsageLookup registerUsageLookup) implements ClassResult<DalvikClassRepresentation> {}