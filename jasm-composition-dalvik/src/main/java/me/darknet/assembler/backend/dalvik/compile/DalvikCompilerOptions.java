package me.darknet.assembler.backend.dalvik.compile;

import me.darknet.assembler.analysis.registry.BasicFieldValueLookup;
import me.darknet.assembler.analysis.registry.BasicMethodValueLookup;
import me.darknet.assembler.analysis.registry.FieldValueLookup;
import me.darknet.assembler.analysis.registry.MethodValueLookup;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dalvik compiler options.
 */
public class DalvikCompilerOptions implements CompilerOptions<DalvikCompilerOptions, DalvikClassRepresentation> {
	private static final MethodValueLookup DEFAULT_METHOD_VALUE_LOOKUP = new BasicMethodValueLookup();
	private static final FieldValueLookup DEFAULT_FIELD_VALUE_LOOKUP = new BasicFieldValueLookup();

	private int dalvikVersion;
	protected TypeAwareness typeAwareness; // Optional, disabled by default to reduce warning noise.
	private InheritanceChecker inheritanceChecker;
	private MethodValueLookup methodValueLookup;
	private FieldValueLookup fieldValueLookup;
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
		return inheritanceChecker == null ? EmptyInheritanceChecker.INSTANCE : inheritanceChecker;
	}

	/**
	 * @param lookup
	 * 		Method value lookup to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull DalvikCompilerOptions withMethodValueLookup(@NotNull MethodValueLookup lookup) {
		this.methodValueLookup = lookup;
		return this;
	}

	/**
	 * @return Configured method value lookup, or the shared basic default.
	 */
	public @NotNull MethodValueLookup getMethodValueLookup() {
		return methodValueLookup == null ? DEFAULT_METHOD_VALUE_LOOKUP : methodValueLookup;
	}

	/**
	 * @param lookup
	 * 		Field value lookup to use for compilation.
	 *
	 * @return This options object.
	 */
	public @NotNull DalvikCompilerOptions withFieldValueLookup(@NotNull FieldValueLookup lookup) {
		this.fieldValueLookup = lookup;
		return this;
	}

	/**
	 * @return Configured field value lookup, or the shared basic default.
	 */
	public @NotNull FieldValueLookup getFieldValueLookup() {
		return fieldValueLookup == null ? DEFAULT_FIELD_VALUE_LOOKUP : fieldValueLookup;
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
