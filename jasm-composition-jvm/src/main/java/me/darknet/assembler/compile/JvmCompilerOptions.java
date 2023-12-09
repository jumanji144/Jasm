package me.darknet.assembler.compile;

import me.darknet.assembler.compile.analysis.VariableNameLookup;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngineFactory;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;

import dev.xdark.blw.version.JavaVersion;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;

public class JvmCompilerOptions implements CompilerOptions<JvmCompilerOptions> {

    protected int asmArgs = 0;
    protected JavaVersion version;
    protected JavaClassRepresentation overlay;
    protected String annotationPath;
    protected InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.INSTANCE;
    protected JvmAnalysisEngineFactory engineProvider = TypedJvmAnalysisEngine::new;

    public JvmCompilerOptions() {
        this.asmArgs = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        this.version = JavaVersion.V8;
    }

    public JvmCompilerOptions computeFrames(boolean computeFrames) {
        if (computeFrames) {
            this.asmArgs |= ClassWriter.COMPUTE_FRAMES;
        } else {
            this.asmArgs &= ~ClassWriter.COMPUTE_FRAMES;
        }
        return this;
    }

    public JvmCompilerOptions computeMaxs(boolean computeMaxs) {
        if (computeMaxs) {
            this.asmArgs |= ClassWriter.COMPUTE_MAXS;
        } else {
            this.asmArgs &= ~ClassWriter.COMPUTE_MAXS;
        }
        return this;
    }

    public JvmCompilerOptions version(int version) {
        this.version = JavaVersion.jdkVersion(version);
        return this;
    }

    public JvmCompilerOptions engineProvider(@NotNull JvmAnalysisEngineFactory engineProvider) {
        this.engineProvider = engineProvider;
        return this;
    }

    public @NotNull JvmAnalysisEngine<?> createEngine(@NotNull VariableNameLookup lookup) {
        return engineProvider.create(lookup);
    }

    @Override
    public int version() {
        return this.version.majorVersion();
    }

    @Override
    public JvmCompilerOptions annotationPath(String path) {
        this.annotationPath = path;
        return this;
    }

    @Override
    public String annotationPath() {
        return this.annotationPath;
    }

    @Override
    public JvmCompilerOptions overlay(ClassRepresentation representation) {
        if (!(representation instanceof JavaClassRepresentation))
            throw new IllegalArgumentException("ClassRepresentation must be a JavaClassRepresentation");
        this.overlay = (JavaClassRepresentation) representation;
        return this;
    }

    @Override
    public ClassRepresentation overlay() {
        return this.overlay;
    }

    @Override
    public InheritanceChecker inheritanceChecker() {
        return this.inheritanceChecker;
    }

    @Override
    public JvmCompilerOptions inheritanceChecker(InheritanceChecker checker) {
        this.inheritanceChecker = checker;
        return this;
    }
}
