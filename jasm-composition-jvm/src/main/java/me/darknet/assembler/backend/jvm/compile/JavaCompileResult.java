package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.backend.jvm.compile.analysis.MethodAnalysisLookup;
import me.darknet.assembler.compiler.ClassResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a Java compilation.
 *
 * @param representation
 * 		The created class.
 * @param analysisLookup
 * 		Lookup to get method stack analysis information for
 * 		declared methods.
 */
public record JavaCompileResult(@Nullable JavaClassRepresentation representation,
                                @NotNull MethodAnalysisLookup analysisLookup) implements ClassResult<JavaClassRepresentation> {}
