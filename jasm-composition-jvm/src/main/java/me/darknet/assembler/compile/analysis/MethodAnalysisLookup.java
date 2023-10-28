package me.darknet.assembler.compile.analysis;

import java.util.Map;

/**
 * Container for one or more {@link AnalysisResults}. Typically, this is implemented as a class, and
 * the look-up keys are method identifiers.
 */
public interface MethodAnalysisLookup {
	/**
	 * @return Map of method keys to their stack analysis results.
	 */
	Map<String, AnalysisResults> allResults();

	/**
	 * @param name Method name.
	 * @param descriptor Method descriptor.
	 * @return Stack analysis results of a given method.
	 */
	AnalysisResults results(String name, String descriptor);
}
