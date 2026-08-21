package me.darknet.assembler.compile.analysis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

/**
 * Container for one or more {@link AnalysisResults}. Typically, this is
 * implemented as a class, and the look-up keys are method identifiers.
 */
public interface MethodAnalysisLookup {
	/**
	 * @return Map of method keys to their stack analysis results.
	 */
	@NotNull
	Map<MethodNode, AnalysisResults> allResults();

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return Stack analysis results of a given method.
	 */
	@Nullable
	AnalysisResults results(String name, String descriptor);

	/**
	 * @param method
	 * 		Method node key.
	 *
	 * @return Stack analysis results of a given method.
	 */
	@Nullable
	AnalysisResults results(MethodNode method);
}
