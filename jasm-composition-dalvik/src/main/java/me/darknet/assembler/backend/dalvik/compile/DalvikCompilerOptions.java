package me.darknet.assembler.backend.dalvik.compile;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dalvik compiler options.
 */
public class DalvikCompilerOptions implements CompilerOptions<DalvikCompilerOptions, DalvikClassRepresentation> {
	private int dalvikVersion;
	protected TypeAwareness typeAwareness; // Optional, disabled by default to reduce warning noise.
	private InheritanceChecker inheritanceChecker;
	private DalvikClassRepresentation overlay;
	private String annotationPath;

	@Override
	public @NotNull DalvikCompilerOptions withOverlay(@Nullable DalvikClassRepresentation representation) {
		this.overlay = representation;
		return this;
	}

	@Override
	public @Nullable DalvikClassRepresentation getOverlay() {
		return overlay;
	}

	/**
	 * @param version
	 * 		Dalvik version to target.
	 *
	 * @return This options object.
	 */
	public @NotNull DalvikCompilerOptions withVersion(int version) {
		this.dalvikVersion = version;
		return this;
	}

	/**
	 * @return Configured Dalvik version to target.
	 */
	public int getVersion() {
		return dalvikVersion;
	}

	/**
	 * @param path
	 * 		Path to the annotation file to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull DalvikCompilerOptions withAnnotationPath(String path) {
		this.annotationPath = path;
		return this;
	}

	/**
	 * @return Configured annotation file path to use for compilation.
	 */
	public String getAnnotationPath() {
		// TODO: Not actually used anywhere yet, as we don't support "just annotation editing" in isolation yet.
		return annotationPath;
	}

	/**
	 * @param checker
	 * 		Inheritance checker to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull DalvikCompilerOptions withInheritanceChecker(@NotNull InheritanceChecker checker) {
		this.inheritanceChecker = checker;
		return this;
	}

	/**
	 * @return Configured inheritance checker to use for compilation.
	 */
	public @NotNull InheritanceChecker getInheritanceChecker() {
		if (inheritanceChecker == null)
			throw new IllegalStateException("Inheritance checker is not set");
		return inheritanceChecker;
	}

	/**
	 * @return Configured type awareness to use for compilation.
	 */
	public TypeAwareness getTypeAwareness() {
		return typeAwareness;
	}

	/**
	 * @param awareness
	 * 		Type awareness to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull DalvikCompilerOptions withTypeAwareness(TypeAwareness awareness) {
		this.typeAwareness = awareness;
		return this;
	}
}
