package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.dex.tree.definitions.instructions.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Control flow graph of a Dalvik method, as built by {@link DalvikControlFlowGraphBuilder}.
 *
 * @param entryIndex
 * 		Index of the first executable instruction, or {@code null} when the code contains none.
 * @param normalSuccessors
 * 		Normal successors of every executable instruction, keyed by its index.
 * 		Instructions that cannot continue normally, such as returns and throws, map to an empty list.
 * 		Explicit targets are listed first, with switch targets in case order, and the fallthrough instruction, if any, is listed last.
 * 		A successor reached both ways is only listed once, at its first position.
 * @param exceptionalSuccessors
 * 		Exceptional successors of every executable instruction, keyed by its index.
 * 		Instructions outside a well-formed try/catch range, or that cannot throw, map to an empty list.
 * 		The edges of an instruction are ordered the way a handler search has to visit them <i>(by try/catch range, then by handler)</i>.
 * @param reachableIndices
 * 		Indices of the instructions reachable from {@link #entryIndex} along normal and exceptional edges,
 * 		in the order they were discovered.
 * @param failures
 * 		Failures encountered while building the graph. Any failure means the graph is incomplete.
 */
record DalvikControlFlowGraph(@Nullable Integer entryIndex,
                              @NotNull Map<Integer, List<Integer>> normalSuccessors,
                              @NotNull Map<Integer, List<ExceptionEdge>> exceptionalSuccessors,
                              @NotNull Set<Integer> reachableIndices,
                              @NotNull List<DalvikAnalysisFailure> failures) {
	/**
	 * Copies every collection so the graph is an immutable snapshot of the builder's working state.
	 */
	public DalvikControlFlowGraph {
		normalSuccessors = immutableLists(normalSuccessors);
		exceptionalSuccessors = immutableLists(exceptionalSuccessors);
		reachableIndices = Collections.unmodifiableSet(new LinkedHashSet<>(reachableIndices));
		failures = List.copyOf(failures);
	}

	/**
	 * @param source
	 * 		Map of successor lists to copy.
	 *
	 * @return Unmodifiable copy of the map in iteration order, with every list copied as well.
	 */
	private static <T> Map<Integer, List<T>> immutableLists(Map<Integer, List<T>> source) {
		Map<Integer, List<T>> copy = new LinkedHashMap<>(source.size());
		source.forEach((index, edges) -> copy.put(index, List.copyOf(edges)));
		return Collections.unmodifiableMap(copy);
	}

	/**
	 * An exceptional flow edge, from an instruction covered by a try/catch range to one of its handlers.
	 * <p>
	 * Edges of an instruction are ordered so the first one whose {@link #exceptionType()} matches the thrown
	 * exception is the handler Dalvik would select, and a {@code null} type matches every exception.
	 *
	 * @param targetIndex
	 * 		Index of the first executable instruction at the handler label.
	 * @param exceptionType
	 * 		Caught exception type, or {@code null} for a handler that catches every exception.
	 * @param tryCatchOrder
	 * 		Position of the try/catch range in the method's try/catch list <i>(the order ranges are searched in)</i>.
	 * @param handlerOrder
	 * 		Position of the handler within its try/catch range <i>(the order handlers are searched in)</i>.
	 * @param handlerLabel
	 * 		Label the handler was declared at, kept for diagnostics.
	 *        {@link #targetIndex} is the first executable instruction at or after it.
	 */
	record ExceptionEdge(int targetIndex, @Nullable ClassDescriptor exceptionType,
	                     int tryCatchOrder, int handlerOrder, @NotNull Label handlerLabel) {}
}
