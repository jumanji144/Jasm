package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.classfile.MemberIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    Map<MemberIdentifier, AnalysisResults> allResults();

    /**
     * @param name
     *                   Method name.
     * @param descriptor
     *                   Method descriptor.
     *
     * @return Stack analysis results of a given method.
     */
    @Nullable
    AnalysisResults results(String name, String descriptor);

    /**
     * @param identifier
     *                   Method identifier.
     *
     * @return Stack analysis results of a given method.
     */
    @Nullable
    AnalysisResults results(MemberIdentifier identifier);
}
