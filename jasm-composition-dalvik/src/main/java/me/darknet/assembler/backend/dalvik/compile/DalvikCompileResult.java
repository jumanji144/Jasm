package me.darknet.assembler.backend.dalvik.compile;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
import org.jetbrains.annotations.Nullable;

/**
 * Dalvik class compilation result.
 *
 * @param representation
 * 		Target representation, or {@code null} when compilation failed.
 */
public record DalvikCompileResult(@Nullable DalvikClassRepresentation representation) implements ClassResult<DalvikClassRepresentation> {}

// TODO: Dalvik doesn't have an engine that does register analysis, like how the JVM has stack value analysis.
//  - We should implement a register analysis engine for Dalvik, and then add a MethodAnalysisLookup to this record, similar to how the JVM does it.
//  - If possible, we should make a 'jasm-composition' base module for any shared analysis models.