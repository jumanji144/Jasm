package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.backend.jvm.compile.analysis.MethodAnalysisLookup;
import me.darknet.assembler.compiler.ClassResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a JVM class compilation.
 *
 * @param representation
 * 		Emitted class file, or {@code null} when compilation failed.
 * @param analysisLookup
 * 		Lookup for stack-analysis results associated with declared methods.
 */
public record JavaCompileResult(@Nullable JavaClassRepresentation representation,
                                @NotNull MethodAnalysisLookup analysisLookup) implements ClassResult<JavaClassRepresentation> {}
