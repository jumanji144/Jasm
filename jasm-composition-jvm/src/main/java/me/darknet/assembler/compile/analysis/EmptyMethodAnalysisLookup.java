package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.classfile.MemberIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Dummy lookup which provides no data.
 */
public class EmptyMethodAnalysisLookup implements MethodAnalysisLookup {
    private static final EmptyMethodAnalysisLookup instance = new EmptyMethodAnalysisLookup();

    /**
     * @return Singleton instance.
     */
    @NotNull
    public static EmptyMethodAnalysisLookup instance() {
        return instance;
    }

    private EmptyMethodAnalysisLookup() {
    }

    @Override
    public @NotNull Map<MemberIdentifier, AnalysisResults> allResults() {
        return Collections.emptyMap();
    }

    @Override
    public @Nullable AnalysisResults results(String name, String descriptor) {
        return null;
    }

    @Override
    public @Nullable AnalysisResults results(MemberIdentifier identifier) {
        return null;
    }
}
