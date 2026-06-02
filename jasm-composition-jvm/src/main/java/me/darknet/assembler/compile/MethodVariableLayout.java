package me.darknet.assembler.compile;

import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.util.JvmTypeUtils;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Canonical parameter layout for a JVM method.
 * <p>
 * This class exists because the same conceptual parameters appear in three different index spaces
 * throughout the pipeline:
 * <ul>
 *     <li>Source parameter order, which follows printed JASM syntax and includes implicit receivers as
 *     an explicit leading {@code this} entry for instance methods.</li>
 *     <li>JVM parameter metadata order, which is what ASM exposes for {@code MethodParameters} and
 *     parameter-annotation arrays, and which never includes the implicit receiver.</li>
 *     <li>Local variable slots, which are what code generation, analysis, and instruction printing
 *     actually operate on, including receiver occupancy and wide-slot expansion for
 *     {@code long}/{@code double}.</li>
 * </ul>
 * Instances are built either from AST-time method information, where source names are fed in directly,
 * or from a compiled {@link MethodNode}, where names are reconstructed from {@code MethodParameters}
 * first and the local variable table second.
 */
public final class MethodVariableLayout {
	private final boolean hasReceiver;
	private final @NotNull Type ownerType;
	private final @NotNull Type methodType;
	private final @NotNull List<SourceParameter> sourceParameters = new ArrayList<>();

	private MethodVariableLayout(@NotNull Type ownerType, @NotNull Type methodType, boolean isStatic) {
		this.ownerType = ownerType;
		this.methodType = methodType;
		this.hasReceiver = !isStatic;
		buildParameters();
	}

	/**
	 * Creates a layout from AST-time method information.
	 * <p>
	 * This form knows the owner type, method descriptor, and whether the method is static,
	 * but it does not yet have any recovered parameter metadata from bytecode. Source names
	 * are expected to be filled in later via {@link #setSourceParameterName(int, String)}.
	 *
	 * @param ownerType
	 * 		Type of the method's declaring class.
	 * @param methodType
	 * 		Type of the method itself.
	 * @param isStatic
	 *        {@code true} when the method is static, {@code false} otherwise.
	 *
	 * @return Variable layout helper.
	 */
	public static @NotNull MethodVariableLayout fromAst(@NotNull Type ownerType, @NotNull Type methodType, boolean isStatic) {
		return new MethodVariableLayout(ownerType, methodType, isStatic);
	}

	/**
	 * Creates a layout from a compiled method and reconstructs the best available parameter names.
	 * <p>
	 * Name recovery prefers {@code MethodParameters} metadata first and then falls back to the
	 * earliest matching local-variable-table entry for the parameter slot and descriptor.
	 *
	 * @param ownerType
	 * 		Type of the method's declaring class.
	 * @param method
	 * 		The method to extract the parameter layout from.
	 *
	 * @return Variable layout helper, with parameter names populated from metadata when available.
	 */
	public static @NotNull MethodVariableLayout fromMethod(@NotNull Type ownerType, @NotNull MethodNode method) {
		MethodVariableLayout layout = new MethodVariableLayout(
				ownerType,
				Type.getMethodType(method.desc),
				(method.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0
		);
		layout.populateNames(method);
		return layout;
	}

	/**
	 * @return {@code true} when the method has an implicit receiver occupying source parameter
	 * index {@code 0} and local slot {@code 0}.
	 */
	public boolean hasReceiver() {
		return hasReceiver;
	}

	/**
	 * @return Number of parameters in source order, including the synthetic leading {@code this}
	 * entry for instance methods.
	 */
	public int sourceParameterCount() {
		return sourceParameters.size();
	}

	/**
	 * @return Number of JVM parameters in descriptor order, excluding any implicit receiver.
	 */
	public int jvmParameterCount() {
		return methodType.getArgumentTypes().length;
	}

	/**
	 * @return Immutable view of the source-ordered parameters that this layout tracks.
	 */
	public @NotNull List<SourceParameter> sourceParameters() {
		return List.copyOf(sourceParameters);
	}

	/**
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 *
	 * @return {@code true} when the given source parameter is the synthetic receiver entry.
	 */
	public boolean isReceiverSourceParameter(int sourceIndex) {
		return hasReceiver && sourceIndex == 0;
	}

	/**
	 * Converts a source-parameter index into the corresponding JVM parameter index.
	 *
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 *
	 * @return JVM parameter index, or {@code -1} when the source parameter is the receiver.
	 */
	public int sourceIndexToJvmParameterIndex(int sourceIndex) {
		return requireSourceParameter(sourceIndex).jvmParameterIndex();
	}

	/**
	 * Converts a JVM parameter index back into source/JASM parameter order.
	 *
	 * @param jvmParameterIndex
	 * 		Index in JVM descriptor / parameter-metadata order.
	 *
	 * @return Source parameter index, accounting for the receiver offset in instance methods.
	 */
	public int jvmParameterIndexToSourceIndex(int jvmParameterIndex) {
		for (SourceParameter parameter : sourceParameters)
			if (parameter.jvmParameterIndex() == jvmParameterIndex)
				return parameter.sourceIndex();
		throw new IllegalArgumentException("Unknown JVM parameter index: " + jvmParameterIndex);
	}

	/**
	 * Converts a source-parameter index into the local slot where that parameter lives at method entry.
	 *
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 *
	 * @return Local slot index, including receiver occupancy and wide-slot expansion.
	 */
	public int sourceIndexToLocalSlot(int sourceIndex) {
		return requireSourceParameter(sourceIndex).localSlot();
	}

	/**
	 * Checks whether a slot/descriptor pair belongs to a declared parameter in this layout.
	 * <p>
	 * This is used to distinguish parameter locals from regular local-variable-table entries during
	 * reanalysis seeding.
	 */
	public boolean matchesParameterSlot(int localSlot, @NotNull String descriptor) {
		for (SourceParameter parameter : sourceParameters)
			if (parameter.localSlot() == localSlot && parameter.type().getDescriptor().equals(descriptor))
				return true;
		return false;
	}

	/**
	 * Records the source-level name for a parameter.
	 * <p>
	 * This is primarily used on the AST compilation path where parameter names are supplied directly
	 * from source syntax.
	 *
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 * @param name
	 * 		The parameter name to record.
	 */
	public void setSourceParameterName(int sourceIndex, @NotNull String name) {
		requireSourceParameter(sourceIndex).setDeclaredName(name);
	}

	/**
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 *
	 * @return The name explicitly recorded for the given source parameter, or {@code null} when no
	 * direct metadata has been provided yet.
	 */
	public @Nullable String declaredSourceParameterName(int sourceIndex) {
		return requireSourceParameter(sourceIndex).declaredName();
	}

	/**
	 * Resolves the effective parameter name for downstream consumers.
	 * <p>
	 * This returns {@code this} for receivers, a recovered/declared name when available, or a stable
	 * fallback generated from slot and type information.
	 *
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 *
	 * @return The canonical name to use for the given source parameter.
	 */
	public @NotNull String sourceParameterName(int sourceIndex) {
		return effectiveName(requireSourceParameter(sourceIndex));
	}

	/**
	 * Builds the emitted {@link ParameterNode} for the given source parameter.
	 *
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 *
	 * @return A {@link ParameterNode} for real JVM parameters, or {@code null} for the receiver or
	 * for parameters whose source name is still unknown.
	 */
	public @Nullable ParameterNode createMethodParameter(int sourceIndex) {
		SourceParameter parameter = requireSourceParameter(sourceIndex);
		if (parameter.receiver() || parameter.declaredName() == null)
			return null;
		return new ParameterNode(parameter.declaredName(), 0);
	}

	/**
	 * Creates the method-entry locals used by assembly-time code generation and reanalysis.
	 * <p>
	 * The returned list is in local-slot order and includes {@code null} placeholders for the
	 * trailing half of wide parameters so it can be passed directly to existing analysis code.
	 *
	 * @return List of method-entry locals, including {@code null} placeholders for wide parameters.
	 */
	public @NotNull List<Local> createParameterLocals() {
		List<Local> parameters = new ArrayList<>();
		for (SourceParameter parameter : sourceParameters) {
			parameters.add(new Local(parameter.localSlot(), effectiveName(parameter), parameter.type()));
			if (JvmTypeUtils.isWide(parameter.type()))
				parameters.add(null);
		}
		return parameters;
	}

	/**
	 * Seeds the parameter portion of a {@link VarCache} using this layout's canonical names and slots.
	 * <p>
	 * This keeps analysis and later local reconstruction aligned with the same parameter mapping used
	 * during initial code generation.
	 *
	 * @param varCache
	 * 		The variable cache to seed with parameter entries.
	 */
	public void seedParameterSlots(@NotNull VarCache varCache) {
		for (SourceParameter parameter : sourceParameters) {
			VarCache.Variable variable = varCache.getOrCreate(
					effectiveName(parameter),
					parameter.localSlot(),
					JvmTypeUtils.isWide(parameter.type())
			);
			variable.updateTypeHint(parameter.type());
		}
	}

	/**
	 * Initializes the raw source/JVM/local-slot mapping from the method descriptor and receiver state.
	 */
	private void buildParameters() {
		int sourceIndex = 0;
		int jvmIndex = 0;
		int localSlot = 0;
		if (hasReceiver) {
			SourceParameter receiver = new SourceParameter(sourceIndex++, -1, localSlot++, ownerType, true);
			receiver.setDeclaredName("this");
			sourceParameters.add(receiver);
		}

		for (Type argumentType : methodType.getArgumentTypes()) {
			sourceParameters.add(new SourceParameter(sourceIndex++, jvmIndex++, localSlot, argumentType, false));
			localSlot += argumentType.getSize();
		}
	}

	/**
	 * Reconstructs parameter names from compiled bytecode metadata.
	 * <p>
	 * The precedence is:
	 * <ol>
	 *     <li>receiver is always named {@code this}</li>
	 *     <li>{@code MethodParameters}</li>
	 *     <li>matching local-variable-table entries</li>
	 * </ol>
	 */
	private void populateNames(@NotNull MethodNode method) {
		List<ParameterNode> methodParameters = method.parameters == null ? List.of() : method.parameters;
		int parameterIndexOffset = Math.max(0, methodParameters.size() - jvmParameterCount());
		for (SourceParameter parameter : sourceParameters) {
			if (parameter.receiver()) {
				parameter.setDeclaredName("this");
				continue;
			}

			int methodParameterIndex = parameter.jvmParameterIndex() + parameterIndexOffset;
			if (methodParameterIndex >= 0 && methodParameterIndex < methodParameters.size()) {
				String parameterName = methodParameters.get(methodParameterIndex).name;
				if (parameterName != null) {
					parameter.setDeclaredName(parameterName);
					continue;
				}
			}

			String localName = findBestLocalVariableName(method, parameter.localSlot(), parameter.type().getDescriptor());
			if (localName != null)
				parameter.setDeclaredName(localName);
		}
	}

	/**
	 * Looks up a source parameter and rejects out-of-range accesses with a descriptive error.
	 *
	 * @param sourceIndex
	 * 		Index in source/JASM parameter order.
	 *
	 * @return The parameter at the given source index.
	 *
	 * @throws IllegalArgumentException
	 * 		when the source index is out of range.
	 */
	private @NotNull SourceParameter requireSourceParameter(int sourceIndex) {
		if (sourceIndex < 0 || sourceIndex >= sourceParameters.size())
			throw new IllegalArgumentException("Unknown source parameter index: " + sourceIndex);
		return sourceParameters.get(sourceIndex);
	}

	/**
	 * Resolves the canonical name that downstream code should use for a parameter.
	 *
	 * @param parameter
	 * 		The parameter to resolve the name for.
	 *
	 * @return The effective name for the parameter.
	 */
	private @NotNull String effectiveName(@NotNull SourceParameter parameter) {
		if (parameter.receiver())
			return "this";

		String declaredName = parameter.declaredName();
		if (declaredName != null)
			return declaredName;

		return VarNaming.name(parameter.localSlot(), parameter.type());
	}

	/**
	 * Finds the most trustworthy LVT name for a parameter slot when {@code MethodParameters}
	 * metadata is absent.
	 * <p>
	 * Matching is restricted to the exact slot and descriptor, and the earliest plausible scope is
	 * preferred so parameter aliases introduced later in the method do not override the true name.
	 *
	 * @param method
	 * 		The method to search for local variable names.
	 * @param localSlot
	 * 		The local slot occupied by the parameter at method entry.
	 * @param descriptor
	 * 		The parameter type descriptor.
	 *
	 * @return The best local variable name for the given slot and descriptor, or {@code null} if no match is found.
	 */
	private static @Nullable String findBestLocalVariableName(@NotNull MethodNode method, int localSlot,
	                                                          @NotNull String descriptor) {
		// Nothing to search when the method has no variable table.
		if (method.localVariables == null || method.localVariables.isEmpty())
			return null;

		return method.localVariables.stream()
				.filter(local -> local.index == localSlot && descriptor.equals(local.desc))
				.min(Comparator.comparingInt((LocalVariableNode local) -> localVariableStartIndex(method, local.start) == 0 ? 0 : 1)
						.thenComparingInt(local -> localVariableStartIndex(method, local.start)))
				.map(local -> local.name)
				.orElse(null);
	}

	/**
	 * Converts an LVT start label into an instruction index for stable ordering comparisons.
	 *
	 * @param method
	 * 		The method containing the instructions and labels.
	 * @param start
	 * 		The label to find the index of.
	 *
	 * @return The instruction index of the label,
	 * or {@code Integer.MAX_VALUE} when the label is not found or the method has no instructions.
	 */
	private static int localVariableStartIndex(@NotNull MethodNode method, @Nullable LabelNode start) {
		if (start == null || method.instructions == null || method.instructions.size() == 0)
			return Integer.MAX_VALUE;

		int index = method.instructions.indexOf(start);
		return index < 0 ? Integer.MAX_VALUE : index;
	}

	/**
	 * One logical source parameter entry.
	 * <p>
	 * For instance methods the first entry represents the implicit receiver, so its
	 * JVM-parameter index is {@code -1} while its local slot is still {@code 0}.
	 */
	public static final class SourceParameter {
		private final int sourceIndex;
		private final int jvmParameterIndex;
		private final int localSlot;
		private final @NotNull Type type;
		private final boolean receiver;
		private @Nullable String declaredName;

		private SourceParameter(int sourceIndex, int jvmParameterIndex, int localSlot, @NotNull Type type, boolean receiver) {
			this.sourceIndex = sourceIndex;
			this.jvmParameterIndex = jvmParameterIndex;
			this.localSlot = localSlot;
			this.type = type;
			this.receiver = receiver;
		}

		/**
		 * @return Parameter index in source/JASM order.
		 */
		public int sourceIndex() {
			return sourceIndex;
		}

		/**
		 * @return Parameter index in JVM metadata order, or {@code -1} for the receiver.
		 */
		public int jvmParameterIndex() {
			return jvmParameterIndex;
		}

		/**
		 * @return First local slot occupied by this parameter at method entry.
		 */
		public int localSlot() {
			return localSlot;
		}

		/**
		 * @return Declared JVM type of the parameter.
		 */
		public @NotNull Type type() {
			return type;
		}

		/**
		 * @return {@code true} when this entry represents the implicit instance-method receiver.
		 */
		public boolean receiver() {
			return receiver;
		}

		/**
		 * @return Name recovered from source or metadata, if one has been assigned.
		 */
		public @Nullable String declaredName() {
			return declaredName;
		}

		private void setDeclaredName(@Nullable String declaredName) {
			this.declaredName = declaredName;
		}
	}
}
