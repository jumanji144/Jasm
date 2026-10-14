package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.MethodReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable lookup of Dalvik analysis results by declaring method.
 */
public final class DalvikMethodAnalysisLookup {
	private static final DalvikMethodAnalysisLookup EMPTY = new DalvikMethodAnalysisLookup(Map.of());

	private final Map<MethodReference, DalvikAnalysisResults> results;

	private DalvikMethodAnalysisLookup(Map<MethodReference, DalvikAnalysisResults> results) {
		this.results = results;
	}

	/**
	 * @return An empty analysis lookup.
	 */
	public static @NotNull DalvikMethodAnalysisLookup empty() {
		return EMPTY;
	}

	/**
	 * @param results
	 * 		Results keyed by their method reference.
	 *
	 * @return Method analysis lookup of the given results.
	 *
	 * @throws IllegalArgumentException
	 * 		When a key does not match its result.
	 */
	public static @NotNull DalvikMethodAnalysisLookup of(@NotNull Map<MethodReference, DalvikAnalysisResults> results) {
		Objects.requireNonNull(results, "results");
		if (results.isEmpty())
			return empty();

		Map<MethodReference, DalvikAnalysisResults> copy = new LinkedHashMap<>();
		String owner = null;
		boolean ownerSet = false;
		for (Map.Entry<MethodReference, DalvikAnalysisResults> entry : results.entrySet()) {
			MethodReference method = Objects.requireNonNull(entry.getKey(), "method key");
			DalvikAnalysisResults analysis = Objects.requireNonNull(entry.getValue(), "analysis result");
			if (!method.equals(analysis.method()))
				throw new IllegalArgumentException("Analysis result method does not match lookup key: " + method);
			if (!ownerSet) {
				owner = method.owner();
				ownerSet = true;
			} else if (!Objects.equals(owner, method.owner())) {
				throw new IllegalArgumentException("Analysis lookup cannot contain methods from multiple owners");
			}
			copy.put(method, analysis);
		}
		return new DalvikMethodAnalysisLookup(Collections.unmodifiableMap(copy));
	}

	/**
	 * @return Map of all results keyed by their method reference.
	 */
	public @NotNull Map<MethodReference, DalvikAnalysisResults> getAllResults() {
		return results;
	}

	/**
	 * @param method
	 * 		Reference of the method to look up.
	 *
	 * @return The result for the method, or {@code null} when absent.
	 */
	public @Nullable DalvikAnalysisResults getResults(@NotNull MethodReference method) {
		return results.get(method);
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return The result for the method, or {@code null} when absent.
	 */
	public @Nullable DalvikAnalysisResults getResults(@NotNull String name, @NotNull String descriptor) {
		for (Map.Entry<MethodReference, DalvikAnalysisResults> entry : results.entrySet()) {
			MethodReference method = entry.getKey();
			if (method.name().equals(name) && method.descriptor().equals(descriptor))
				return entry.getValue();
		}
		return null;
	}
}
