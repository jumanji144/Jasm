package me.darknet.assembler.compile;

import me.darknet.assembler.compile.analysis.MethodAnalysisLookup;
import me.darknet.assembler.compiler.ClassRepresentation;

/**
 * @param classFile
 * 		Raw class file.
 * @param analysisLookup
 * 		Lookup to get method stack analysis information for declared methods.
 */
public record JavaClassRepresentation(byte[] classFile, MethodAnalysisLookup analysisLookup) implements ClassRepresentation {
}
