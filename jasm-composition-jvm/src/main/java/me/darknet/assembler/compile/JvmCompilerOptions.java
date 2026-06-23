package me.darknet.assembler.compile;

import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngineFactory;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;

import java.util.Objects;

public class JvmCompilerOptions implements CompilerOptions<JvmCompilerOptions> {
    private static final int DEFAULT_VERSION = 8;

    // General class options
    protected boolean reuseOverlayPool = true;
    protected int asmArgs;
    protected int version;
    protected JavaClassRepresentation overlay;
    protected String annotationPath;
    protected TypeAwareness typeAwareness; // Optional, disabled by default to reduce warning noise.
    protected InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.INSTANCE;
    protected JvmAnalysisEngineFactory engineProvider = TypedJvmAnalysisEngine::new;

    // Variable writing options
    protected JvmVariableMode variableTableMode = JvmVariableMode.ALWAYS_WRITE;
    protected JvmVariableEmissionFilter variableFilter = JvmVariableEmissionFilter.ALWAYS;

    public JvmCompilerOptions() {
        this.asmArgs = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        this.version = DEFAULT_VERSION;
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
        this.version = version;
        return this;
    }

    public JvmCompilerOptions engineProvider(@NotNull JvmAnalysisEngineFactory engineProvider) {
        this.engineProvider = engineProvider;
        return this;
    }

    public @NotNull JvmAnalysisEngine<?> createEngine(@NotNull VarCache varCache) {
        JvmAnalysisEngine<?> engine = engineProvider.create(varCache);
        engine.setChecker(inheritanceChecker());
        return engine;
    }

    @Override
    public int version() {
        return this.version;
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
    public TypeAwareness awareness() {
        return typeAwareness;
    }

    @Override
    public JvmCompilerOptions awareness(TypeAwareness awareness) {
        this.typeAwareness = awareness;
        return this;
    }

    @Override
    public JvmCompilerOptions inheritanceChecker(InheritanceChecker checker) {
        this.inheritanceChecker = checker;
        return this;
    }

    public JvmVariableMode variableTableMode() {
        return variableTableMode;
    }

    public JvmCompilerOptions variableTableMode(JvmVariableMode variableTableMode) {
        this.variableTableMode = variableTableMode;
        return this;
    }

    public boolean reuseOverlayPool() {
        return reuseOverlayPool;
    }

    public JvmCompilerOptions reuseOverlayPool(boolean reuseOverlayPool) {
        this.reuseOverlayPool = reuseOverlayPool;
        return this;
    }

    public @NotNull JvmVariableEmissionFilter variableFilter() {
        return variableFilter;
    }

    public JvmCompilerOptions variableFilter(@NotNull JvmVariableEmissionFilter writeVariableFilter) {
        this.variableFilter = Objects.requireNonNull(writeVariableFilter, "variableFilter");
        return this;
    }
}
