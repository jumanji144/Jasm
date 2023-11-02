package me.darknet.assembler.compile.builder;

import dev.xdark.blw.classfile.MemberIdentifier;
import dev.xdark.blw.classfile.generic.*;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.MethodType;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.MethodAnalysisLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BlwReplaceClassBuilder extends GenericClassBuilder implements MethodAnalysisLookup {
	private final Map<MemberIdentifier, AnalysisResults> methodAnalysisResults = new HashMap<>();

	@Override
	protected @NotNull GenericFieldBuilder newFieldBuilder(int accessFlags, String name, ClassType type) {
		return super.newFieldBuilder(accessFlags, name, type);
	}

	@Override
	protected @NotNull GenericMethodBuilder newMethodBuilder(int accessFlags, String name, MethodType type) {
		return new BlwReplaceMethodBuilder(accessFlags, name, type);
	}

	@Override
	protected @NotNull GenericRecordComponentBuilder newRecordComponentBuilder(String name, ClassType type, String signature) {
		return super.newRecordComponentBuilder(name, type, signature);
	}

	@Override
	protected @NotNull GenericModuleBuilder newModuleBuilder(String name, int access, @Nullable String version) {
		return super.newModuleBuilder(name, access, version);
	}

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
		return methodAnalysisResults.get(new MemberIdentifier(name,descriptor));
	}
}
