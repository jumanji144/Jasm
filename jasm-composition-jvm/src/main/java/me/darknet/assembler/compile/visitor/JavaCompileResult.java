package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.analysis.MethodAnalysisLookup;
import me.darknet.assembler.compiler.ClassResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param representation
 *                       The created class.
 * @param analysisLookup
 *                       Lookup to get method stack analysis information for
 *                       declared methods.
 */
public record JavaCompileResult(@Nullable JavaClassRepresentation representation,
                                @NotNull MethodAnalysisLookup analysisLookup) implements ClassResult {
}
