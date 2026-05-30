package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Base outline for an engine intended for use in proper stack/local analysis.
 */
public abstract class JvmAnalysisEngine<F extends Frame> implements Opcodes {
	protected static final Type METHOD_TYPE = JvmTypeUtils.METHOD_TYPE;
	protected static final Type METHOD_HANDLE = JvmTypeUtils.METHOD_HANDLE;
	protected static final Type CLASS = JvmTypeUtils.CLASS;

	protected final VarCache varCache;
	protected InheritanceChecker checker;
	protected ErrorCollector errorCollector;
	private MethodAnalysisResult result;
	private AnalysisSession<F> session;

	public JvmAnalysisEngine(@NotNull VarCache varCache) {
		this.varCache = varCache;
		this.result = new MethodAnalysisResult();
	}

	public abstract FrameOps<?> newFrameOps();

	public abstract void execute(@NotNull AbstractInsnNode instruction);

	@NotNull
	public VarCache getVarCache() {
		return varCache;
	}

	public void setChecker(InheritanceChecker checker) {
		this.checker = checker;
	}

	public @Nullable InheritanceChecker getChecker() {
		return checker;
	}

	public void setResult(@NotNull MethodAnalysisResult result) {
		this.result = result;
	}

	public void bindSession(@Nullable AnalysisSession<F> session) {
		this.session = session;
	}

	public final @NotNull MethodAnalysisResult result() {
		return result;
	}

	public final @NotNull AnalysisSession<F> session() {
		if (session == null)
			throw new IllegalStateException("Analysis session not bound");
		return session;
	}

	public final @NotNull F frame() {
		F frame = session().frame();
		if (frame == null)
			throw new IllegalStateException("Active frame not bound");
		return frame;
	}

	public void setErrorCollector(ErrorCollector errorCollector) {
		this.errorCollector = errorCollector;
	}

	public void clearErrorsAt(@NotNull AbstractInsnNode instruction) {
		if (errorCollector == null)
			return;
		ASTInstruction ast = result().getInstructionToAstMap().get(instruction);
		if (ast != null)
			errorCollector.removeAt(ast.location());
	}

	public void warn(@NotNull AbstractInsnNode instruction, @NotNull String message) {
		if (errorCollector == null)
			return;
		ASTInstruction ast = result().getInstructionToAstMap().get(instruction);
		if (ast != null)
			errorCollector.addWarn(message, ast.location());
	}

	protected void error(@NotNull AbstractInsnNode instruction, @NotNull String message) {
		if (errorCollector == null)
			return;
		ASTInstruction ast = result().getInstructionToAstMap().get(instruction);
		if (ast != null)
			errorCollector.addError(message + " @ " + ast.content(), ast.location());
	}

	protected void validateTypeUse(@NotNull AbstractInsnNode instruction, @Nullable Type inputType,
	                               @NotNull Type destinationType, @NotNull String verb, @NotNull String noun) {
		if (inputType == null) {
			if (JvmTypeUtils.isPrimitive(destinationType))
				warn(instruction, "Cannot " + verb + " null value into primitive " + noun);
			return;
		}

		if (JvmTypeUtils.isPrimitive(inputType)) {
			if (JvmTypeUtils.isReference(destinationType))
				warn(instruction, "Cannot " + verb + " primitive value into reference " + noun);
			return;
		}

		if (inputType.getSort() == Type.ARRAY) {
			if (JvmTypeUtils.isPrimitive(destinationType))
				warn(instruction, "Cannot " + verb + " array value into primitive " + noun);
			else if (destinationType.getSort() == Type.ARRAY && !isReferenceAssignable(inputType, destinationType))
				warn(instruction, "Cannot " + verb + " array value into array " + noun + " of different component type or dimension");
			else if (destinationType.getSort() == Type.OBJECT && !isReferenceAssignable(inputType, destinationType))
				warn(instruction, "Cannot " + verb + " array value into " + noun + " that is not 'java/lang/Object'");
			return;
		}

		if (JvmTypeUtils.isPrimitive(destinationType) || destinationType.getSort() == Type.ARRAY)
			warn(instruction, "Cannot " + verb + " instance value into " + noun);
	}

	private boolean isReferenceAssignable(@NotNull Type inputType, @NotNull Type destinationType) {
		if (inputType.equals(destinationType))
			return true;

		if (inputType.getSort() == Type.ARRAY) {
			if (destinationType.getSort() == Type.ARRAY)
				return isArrayAssignable(inputType, destinationType);
			if (destinationType.getSort() == Type.OBJECT)
				return isAssignableViaChecker(inputType, destinationType) || isArraySupertype(destinationType);
			return false;
		}

		if (inputType.getSort() == Type.OBJECT && destinationType.getSort() == Type.OBJECT)
			return isAssignableViaChecker(inputType, destinationType) || destinationType.equals(JvmTypeUtils.OBJECT);

		return false;
	}

	private boolean isArrayAssignable(@NotNull Type inputType, @NotNull Type destinationType) {
		Type inputComponent = immediateComponentType(inputType);
		Type destinationComponent = immediateComponentType(destinationType);
		if (inputComponent.equals(destinationComponent))
			return true;
		if (JvmTypeUtils.isPrimitive(inputComponent) || JvmTypeUtils.isPrimitive(destinationComponent))
			return false;
		return isReferenceAssignable(inputComponent, destinationComponent);
	}

	private boolean isAssignableViaChecker(@NotNull Type inputType, @NotNull Type destinationType) {
		return checker != null && checker.isSubclassOf(
				JvmTypeUtils.internalName(inputType),
				JvmTypeUtils.internalName(destinationType)
		);
	}

	private static boolean isArraySupertype(@NotNull Type type) {
		String internalName = type.getInternalName();
		return "java/lang/Object".equals(internalName) ||
				"java/lang/Cloneable".equals(internalName) ||
				"java/io/Serializable".equals(internalName);
	}

	private static @NotNull Type immediateComponentType(@NotNull Type arrayType) {
		return Type.getType(arrayType.getDescriptor().substring(1));
	}
}
