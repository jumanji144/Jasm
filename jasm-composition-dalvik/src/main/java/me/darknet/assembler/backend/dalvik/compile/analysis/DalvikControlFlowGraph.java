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

// TODO: Document components
record DalvikControlFlowGraph(@Nullable Integer entryIndex,
                              @NotNull Map<Integer, List<Integer>> normalSuccessors,
                              @NotNull Map<Integer, List<ExceptionEdge>> exceptionalSuccessors,
                              @NotNull Set<Integer> reachableIndices,
                              @NotNull List<DalvikAnalysisFailure> failures) {
    DalvikControlFlowGraph {
        normalSuccessors = immutableLists(normalSuccessors);
        exceptionalSuccessors = immutableLists(exceptionalSuccessors);
        reachableIndices = Collections.unmodifiableSet(new LinkedHashSet<>(reachableIndices));
        failures = List.copyOf(failures);
    }

    private static <T> Map<Integer, List<T>> immutableLists(Map<Integer, List<T>> source) {
        Map<Integer, List<T>> copy = new LinkedHashMap<>(source.size());
        source.forEach((index, edges) -> copy.put(index, List.copyOf(edges)));
        return Collections.unmodifiableMap(copy);
    }


	// TODO: Document components
    record ExceptionEdge(int targetIndex, @Nullable ClassDescriptor exceptionType,
                         int tryCatchOrder, int handlerOrder, @NotNull Label handlerLabel) {}
}
