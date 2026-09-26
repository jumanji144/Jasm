package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.backend.jvm.compile.analysis.VarCache;
import me.darknet.assembler.backend.jvm.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.backend.jvm.compile.analysis.jvm.JvmAnalysisEngineFactory;
import me.darknet.assembler.backend.jvm.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.backend.jvm.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Outcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.List;
import java.util.Objects;

/**
 * JVM compiler options.
 */
public class JvmCompilerOptions implements CompilerOptions<JvmCompilerOptions, JavaClassRepresentation> {
	private static final int DEFAULT_VERSION = 8;

	// General class options
	protected boolean reuseOverlayPool = true;
	protected int asmArgs;
	protected int version;
	protected JavaClassRepresentation overlay;
	protected String annotationPath;
	protected @Nullable AnnotationTarget annotationTarget;
	protected TypeAwareness typeAwareness; // Optional, disabled by default to reduce warning noise.
	protected InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.INSTANCE;
	protected JvmAnalysisEngineFactory engineProvider = TypedJvmAnalysisEngine::new;
	protected boolean verifyOutput = true;

	// Variable writing options
	protected JvmVariableMode variableTableMode = JvmVariableMode.ALWAYS_WRITE;
	protected JvmVariableEmissionFilter variableFilter = JvmVariableEmissionFilter.ALWAYS;

	public JvmCompilerOptions() {
		this.asmArgs = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
		this.version = DEFAULT_VERSION;
	}

	/**
	 * @param varCache
	 * 		Cache to use for variable table generation.
	 *
	 * @return New analysis engine instance.
	 *
	 * @see #withEngineProvider(JvmAnalysisEngineFactory)
	 */
	public @NotNull JvmAnalysisEngine<?> createEngine(@NotNull VarCache varCache) {
		JvmAnalysisEngine<?> engine = engineProvider.create(varCache);
		engine.setChecker(getInheritanceChecker());
		return engine;
	}

	@Override
	public @NotNull JvmCompilerOptions withOverlay(JavaClassRepresentation representation) {
		this.overlay = representation;
		return this;
	}

	@Override
	public JavaClassRepresentation getOverlay() {
		return this.overlay;
	}

	/**
	 * @param engineProvider
	 * 		The analysis engine provider to use for compilation.
	 *
	 * @return This options object.
	 *
	 * @see TypedJvmAnalysisEngine
	 * @see ValuedJvmAnalysisEngine
	 */
	public @NotNull JvmCompilerOptions withEngineProvider(@NotNull JvmAnalysisEngineFactory engineProvider) {
		this.engineProvider = engineProvider;
		return this;
	}

	/**
	 * @param computeFrames
	 *        {@code true} to compute stack frames, {@code false} to leave frames as-is.
	 * 		For simple edits that don't change the control flow, this can generally be left as {@code false} to avoid unnecessary overhead.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withComputeFrames(boolean computeFrames) {
		if (computeFrames) {
			this.asmArgs |= ClassWriter.COMPUTE_FRAMES;
		} else {
			this.asmArgs &= ~ClassWriter.COMPUTE_FRAMES;
		}
		return this;
	}

	/**
	 * @param computeMaxs
	 *        {@code true} to compute max stack and locals, {@code false} to leave them as-is.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withComputeMaxs(boolean computeMaxs) {
		if (computeMaxs) {
			this.asmArgs |= ClassWriter.COMPUTE_MAXS;
		} else {
			this.asmArgs &= ~ClassWriter.COMPUTE_MAXS;
		}
		return this;
	}

	/**
	 * @param version
	 * 		Java version to target for compilation. For example, to target Java 8, use {@code 8}.
	 *
	 * @return This options object.
	 */
	@Deprecated // TODO: Strictly use the source 'version' from the AST instead of this option.
	public @NotNull JvmCompilerOptions withVersion(int version) {
		this.version = version;
		return this;
	}

	/**
	 * @param verifyOutput
	 *        {@code true} to verify the output class after compilation, {@code false} to skip verification.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withOutputVerification(boolean verifyOutput) {
		this.verifyOutput = verifyOutput;
		return this;
	}

	/**
	 * @return {@code true} if the output class should be verified after compilation, {@code false} to skip verification.
	 */
	public boolean verifyOutput() {
		return verifyOutput;
	}

	/**
	 * @return The Java version to target for compilation.
	 */
	@Deprecated // TODO: Strictly use the source 'version' from the AST instead of this option.
	public int getVersion() {
		return this.version;
	}

	/**
	 * @param path
	 * 		Path to the annotation target to use for compilation, or {@code null} to clear it.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withAnnotationPath(@Nullable String path) {
		this.annotationPath = path;
		return this;
	}

	/**
	 * @return Configured annotation target path, or {@code null} when none was set.
	 */
	public @Nullable String getAnnotationPath() {
		return annotationPath;
	}

	/**
	 * @return Resolved annotation target, or {@code null} when no path was configured or the path could not be parsed.
	 */
	public @Nullable AnnotationTarget getAnnotationTarget() {
		return annotationTarget;
	}

	/**
	 * Resolves the configured annotation target path.
	 *
	 * @return Diagnostics describing an unusable path, or an empty list when the path resolves or none was configured.
	 */
	public @NotNull List<Diagnostic> resolveAnnotationTarget() {
		if (annotationPath == null) {
			annotationTarget = null;
			return List.of();
		}
		Outcome<AnnotationTarget> parsed = AnnotationTarget.parse(annotationPath);
		annotationTarget = parsed.hasErrors() ? null : parsed.requireValue();
		return parsed.diagnostics();
	}

	/**
	 * @return The common-type inheritance checker to use for compilation.
	 */
	public @NotNull InheritanceChecker getInheritanceChecker() {
		if (inheritanceChecker == null)
			throw new IllegalStateException("Inheritance checker is not set");
		return inheritanceChecker;
	}

	/**
	 * @return The type-awareness provider to use for compilation.
	 */
	public TypeAwareness getTypeAwareness() {
		return typeAwareness;
	}

	/**
	 * @param awareness
	 * 		The type-awareness provider to use for compilation. If {@code null}, type-awareness will be disabled.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withTypeAwareness(TypeAwareness awareness) {
		this.typeAwareness = awareness;
		return this;
	}

	/**
	 * @param checker
	 * 		The common-type inheritance checker to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withInheritanceChecker(@NotNull InheritanceChecker checker) {
		this.inheritanceChecker = checker;
		return this;
	}

	/**
	 * @return The variable table writing mode to use for compilation.
	 */
	public JvmVariableMode getVariableTableMode() {
		return variableTableMode;
	}

	/**
	 * @param variableTableMode
	 * 		The variable table writing mode to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withVariableTableMode(JvmVariableMode variableTableMode) {
		this.variableTableMode = variableTableMode;
		return this;
	}

	/**
	 * @return {@code true} when the overlay class's constant-pool should be reused as-is when compiling.
	 * {@code false} when a new constant-pool should be created for the output class, even if an overlay is provided.
	 *
	 * @see ClassWriter#ClassWriter(int)
	 * @see ClassWriter#ClassWriter(ClassReader, int)
	 */
	public boolean reuseOverlayPool() {
		return reuseOverlayPool;
	}

	/**
	 * @param reuseOverlayPool
	 *        {@code true} when the overlay class's constant-pool should be reused as-is when compiling.
	 *        {@code false} when a new constant-pool should be created for the output class, even if an overlay is provided.
	 *
	 * @return This options object.
	 *
	 * @see ClassWriter#ClassWriter(int)
	 * @see ClassWriter#ClassWriter(ClassReader, int)
	 */
	public @NotNull JvmCompilerOptions withReuseOverlayPool(boolean reuseOverlayPool) {
		this.reuseOverlayPool = reuseOverlayPool;
		return this;
	}

	/**
	 * @return The variable emission filter to use for compilation.
	 * This filter determines which variables are written to the output class's variable table.
	 */
	public @NotNull JvmVariableEmissionFilter getVariableFilter() {
		return variableFilter;
	}

	/**
	 * @param writeVariableFilter
	 * 		The variable emission filter to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull JvmCompilerOptions withVariableFilter(@NotNull JvmVariableEmissionFilter writeVariableFilter) {
		this.variableFilter = Objects.requireNonNull(writeVariableFilter, "variableFilter");
		return this;
	}
}
