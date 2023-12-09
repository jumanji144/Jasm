package me.darknet.assembler.compile.builder;

import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.MethodAnalysisLookup;

import dev.xdark.blw.classfile.MemberIdentifier;
import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import dev.xdark.blw.classfile.generic.GenericMethodBuilder;
import dev.xdark.blw.type.MethodType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BlwReplaceClassBuilder extends GenericClassBuilder implements MethodAnalysisLookup {
    private final Map<MemberIdentifier, AnalysisResults> methodAnalysisResults = new HashMap<>();

    @NotNull
    public Map<MemberIdentifier, AnalysisResults> getMethodAnalysisResults() {
        return methodAnalysisResults;
    }

    public void setMethodAnalysis(String name, MethodType type, AnalysisResults results) {
        methodAnalysisResults.put(new MemberIdentifier(name, type), results);
    }

    @Override
    public @NotNull Map<MemberIdentifier, AnalysisResults> allResults() {
        return methodAnalysisResults;
    }

    @Override
    public AnalysisResults results(String name, String descriptor) {
        return results(new MemberIdentifier(name, descriptor));
    }

    @Override
    public @Nullable AnalysisResults results(MemberIdentifier identifier) {
        return methodAnalysisResults.get(identifier);
    }

    @Override
    protected @NotNull GenericMethodBuilder newMethodBuilder(int accessFlags, String name, MethodType type) {
        return new BlwReplaceMethodBuilder(accessFlags, name, type);
    }
}
