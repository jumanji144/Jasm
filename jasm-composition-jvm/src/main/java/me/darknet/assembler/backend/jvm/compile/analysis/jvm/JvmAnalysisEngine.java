package me.darknet.assembler.backend.jvm.compile.analysis.jvm;

import java.util.IdentityHashMap;
import java.util.Map;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.backend.jvm.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.backend.jvm.compile.analysis.VarCache;
import me.darknet.assembler.backend.jvm.compile.analysis.frame.Frame;
import me.darknet.assembler.backend.jvm.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.backend.jvm.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Base outline for an engine intended for use in proper stack/local analysis.
 */
public abstract class JvmAnalysisEngine<F extends Frame> implements Opcodes {
	protected static final Type METHOD_TYPE = JvmTypeUtils.METHOD_TYPE;
	protected static final Type METHOD_HANDLE = JvmTypeUtils.METHOD_HANDLE;
	protected static final Type CLASS = JvmTypeUtils.CLASS;

	protected final Map<AbstractInsnNode, Integer> allocationIdentities = new IdentityHashMap<>();
	protected final VarCache varCache;
	protected InheritanceChecker checker;
	protected DiagnosticSink sink;
	private MethodAnalysisResult result;
	private AnalysisSession<F> session;
	private Type analyzedReturnType = JvmTypeUtils.VOID;
	private String analyzedOwner;
	private String analyzedMethodName;
	private int analyzedAccess;
	private int nextAllocationIdentity;

	public JvmAnalysisEngine(@NotNull VarCache varCache) {
		this.varCache = varCache;
		this.result = new MethodAnalysisResult();
	}

	/**
	 * @return New frame operations instance for this engine.
	 */
	public abstract @NotNull FrameOps<?> newFrameOps();

	/**
	 * Execute analysis for the given instruction. Called on each instruction in the method being analyzed.
	 *
	 * @param instruction
	 * 		Instruction to analyze.
	 *
	 * @see JvmAnalysisRunner
	 */
	public void execute(@NotNull AbstractInsnNode instruction) {
		switch (instruction) {
			case InsnNode insnNode -> executeSimple(insnNode);
			case IntInsnNode intInsnNode -> executeIntInsn(intInsnNode);
			case LdcInsnNode ldcInsnNode -> executeLdcInsn(ldcInsnNode);
			case VarInsnNode varInsnNode -> executeVarInsn(varInsnNode);
			case IincInsnNode iincInsnNode -> executeIincInsn(iincInsnNode);
			case TypeInsnNode typeInsnNode -> executeTypeInsn(typeInsnNode);
			case MethodInsnNode methodInsnNode -> executeMethodInsn(methodInsnNode);
			case FieldInsnNode fieldInsnNode -> executeFieldInsn(fieldInsnNode);
			case InvokeDynamicInsnNode invokeDynamicInsnNode -> executeInvokeDynamicInsn(invokeDynamicInsnNode);
			case JumpInsnNode jumpInsnNode -> executeJumpInsn(jumpInsnNode);
			case LookupSwitchInsnNode lookupSwitchInsnNode -> executeSwitchInsn(lookupSwitchInsnNode);
			case TableSwitchInsnNode tableSwitchInsnNode -> executeSwitchInsn(tableSwitchInsnNode);
			case MultiANewArrayInsnNode multiANewArrayInsnNode -> executeMultiANewArrayInsn(multiANewArrayInsnNode);
			default -> {}
		}
	}

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeSimple(@NotNull InsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeIntInsn(@NotNull IntInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeLdcInsn(@NotNull LdcInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeVarInsn(@NotNull VarInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeIincInsn(@NotNull IincInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeTypeInsn(@NotNull TypeInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeMethodInsn(@NotNull MethodInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeFieldInsn(@NotNull FieldInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeInvokeDynamicInsn(@NotNull InvokeDynamicInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeJumpInsn(@NotNull JumpInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeSwitchInsn(@NotNull AbstractInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instruction to analyze.
	 */
	protected abstract void executeMultiANewArrayInsn(@NotNull MultiANewArrayInsnNode instruction);

	/**
	 * @return Variable cache for this engine.
	 */
	@NotNull
	public VarCache getVarCache() {
		return varCache;
	}

	/**
	 * @param checker
	 * 		Inheritance checker to associate with this engine.
	 * 		This is used for reference type assignability checks.
	 */
	public void setChecker(InheritanceChecker checker) {
		this.checker = checker;
	}

	/**
	 * @return Associated inheritance checker for this engine, or {@code null} if none is bound.
	 */
	public @Nullable InheritanceChecker getChecker() {
		return checker;
	}

	/**
	 * Set the result holder for this engine to write analysis results to.
	 * At this point the result is not yet finalized. The engine will write to the result as it analyzes the method.
	 *
	 * @param result
	 * 		Result to write analysis results to.
	 *
	 * @see JvmAnalysisRunner#execute(JvmAnalysisEngine, JvmAnalysisRunner.Info, MethodAnalysisResult)
	 */
	public void setResult(@NotNull MethodAnalysisResult result) {
		this.result = result;
	}

	/**
	 * The result is mutable and will be written to as the engine analyzes the method.
	 *
	 * @return Analysis result for the method being analyzed.
	 */
	public final @NotNull MethodAnalysisResult getResult() {
		return result;
	}

	/**
	 * Set the analysis session to this engine for analysis.
	 *
	 * @param session
	 * 		Session to assign.
	 */
	public void setSession(@Nullable AnalysisSession<F> session) {
		this.session = session;
	}

	/**
	 * The session is mutable and will be written to as the engine analyzes the method.
	 *
	 * @return Analysis session for this engine.
	 */
	public @NotNull AnalysisSession<F> getSession() {
		if (session == null)
			throw new IllegalStateException("Analysis session not bound");
		return session;
	}

	/**
	 * Set the method details of the analyzed method.
	 *
	 * @param returnType
	 * 		Method return type.
	 * @param owner
	 * 		Method owner type.
	 * @param methodName
	 * 		Method name.
	 * @param access
	 * 		Method access flags.
	 */
	public void setMethodDetails(@NotNull Type returnType, @Nullable String owner, @Nullable String methodName, int access) {
		this.analyzedReturnType = returnType;
		this.analyzedOwner = owner;
		this.analyzedMethodName = methodName;
		this.analyzedAccess = access;
		this.nextAllocationIdentity = 1;
		this.allocationIdentities.clear();
	}

	/**
	 * @param owner
	 * 		Type of the owner of the uninitialized type.
	 *
	 * @return New uninitialized type for the given owner.
	 */
	protected @NotNull Type newUninitializedType(@NotNull Type owner) {
		return JvmTypeUtils.uninitializedType(owner, nextAllocationIdentity++);
	}

	/**
	 * Creates an uninitialized type whose allocation identity is stable across re-executions of the same
	 * {@code new} instruction. Re-running the same allocation site must yield the same verifier marker,
	 * otherwise re-visits during worklist analysis merge two distinct markers for one allocation and degrade
	 * the value to {@code TOP}.
	 *
	 * @param owner
	 * 		Type of the owner of the uninitialized type.
	 * @param site
	 * 		The {@code new} instruction that allocates this value.
	 *
	 * @return Uninitialized type for the given allocation site.
	 */
	protected @NotNull Type newUninitializedType(@NotNull Type owner, @NotNull AbstractInsnNode site) {
		Integer identity = allocationIdentities.get(site);
		if (identity == null) {
			identity = nextAllocationIdentity++;
			allocationIdentities.put(site, identity);
		}
		return JvmTypeUtils.uninitializedType(owner, identity);
	}

	/**
	 * @return Return type of the analyzed method.
	 */
	protected @NotNull Type analyzedReturnType() {
		return analyzedReturnType;
	}

	/**
	 * @return Owner type of the analyzed method.
	 */
	protected @Nullable String analyzedOwner() {
		return analyzedOwner;
	}

	/**
	 * @return {@code true} if the analyzed method is a constructor, {@code false} otherwise.
	 */
	protected boolean isConstructor() {
		return "<init>".equals(analyzedMethodName);
	}

	/**
	 * @return Access flags of the analyzed method.
	 */
	protected int analyzedAccess() {
		return analyzedAccess;
	}

	/**
	 * @return Current active frame for this engine.
	 *
	 * @throws IllegalStateException
	 * 		When no active frame is bound to the engine.
	 */
	public final @NotNull F getCurrentFrame() {
		F frame = getSession().frame();
		if (frame == null)
			throw new IllegalStateException("Active frame not bound");
		return frame;
	}

	/**
	 * @param sink
	 * 		Diagnostic sink to associate with this engine.
	 */
	public void setDiagnosticSink(@Nullable DiagnosticSink sink) {
		this.sink = sink;
	}

	/**
	 * Clears {@link DiagnosticPhase#OUTPUT_VERIFICATION} diagnostics this analysis pass reported for the given instruction.
	 *
	 * @param instruction
	 * 		Instruction to clear diagnostics for.
	 */
	public void clearErrorsAt(@NotNull AbstractInsnNode instruction) {
		if (sink == null)
			return;
		ASTInstruction ast = getResult().getExecutableInstructionToAstMap().get(instruction);
		if (ast != null)
			sink.removeAt(ast.location(), DiagnosticPhase.OUTPUT_VERIFICATION);
	}

	/**
	 * Add a warning message associated with the given instruction.
	 *
	 * @param instruction
	 * 		Instruction to associate the warning with.
	 * @param message
	 * 		Warning message to add.
	 */
	public void warn(@NotNull AbstractInsnNode instruction, @NotNull String message) {
		if (sink == null)
			return;
		ASTInstruction ast = getResult().getExecutableInstructionToAstMap().get(instruction);
		if (ast != null)
			sink.warning(DiagnosticPhase.OUTPUT_VERIFICATION, DiagnosticCode.VERIFICATION_WARNING, message,
					ast.location());
	}

	/**
	 * Add an error message associated with the given instruction.
	 *
	 * @param instruction
	 * 		Instruction to associate the error with.
	 * @param message
	 * 		Error message to add.
	 */
	protected void error(@NotNull AbstractInsnNode instruction, @NotNull String message) {
		if (sink == null)
			return;
		ASTInstruction ast = getResult().getExecutableInstructionToAstMap().get(instruction);
		if (ast != null)
			sink.error(DiagnosticPhase.OUTPUT_VERIFICATION, DiagnosticCode.ANALYSIS_FAILURE,
					message + " @ " + ast.content(), ast.location());
	}

	/**
	 * Validate that the input type can be used as the destination type.
	 *
	 * @param instruction
	 * 		Instruction to associate the warning with.
	 * @param inputType
	 * 		Input type to validate.
	 * @param destinationType
	 * 		Destination type to validate against.
	 * @param verb
	 * 		Action verb to use in the warning message.
	 * @param noun
	 * 		Noun to use in the warning message.
	 */
	protected void validateTypeUse(@NotNull AbstractInsnNode instruction, @Nullable Type inputType,
	                               @NotNull Type destinationType, @NotNull String verb, @NotNull String noun) {
		if (inputType == null) {
			if (JvmTypeUtils.isPrimitive(destinationType))
				warn(instruction, "Cannot " + verb + " null value into primitive " + noun);
			return;
		}

		if (JvmTypeUtils.isUninitialized(inputType)) {
			warn(instruction, "Cannot use uninitialized object as " + noun);
			return;
		}

		if (JvmTypeUtils.isPrimitive(inputType)) {
			Type actual = JvmTypeUtils.verificationType(inputType);
			Type expected = JvmTypeUtils.verificationType(destinationType);
			if (!JvmTypeUtils.isPrimitive(destinationType) || !expected.equals(actual))
				warn(instruction, "Cannot " + verb + " " + JvmTypeUtils.displayName(inputType)
						+ " into " + noun + " of type " + destinationType.getDescriptor());
			return;
		}

		if (!JvmTypeUtils.isReference(inputType) || !JvmTypeUtils.isReference(destinationType)
				|| !isReferenceAssignable(inputType, destinationType))
			warn(instruction, "Cannot " + verb + " " + JvmTypeUtils.displayName(inputType)
					+ " into " + noun + " of type " + destinationType.getDescriptor());
	}

	/**
	 * Warn when a local-variable store does not have enough values on the operand stack.
	 *
	 * @param instruction
	 * 		Instruction to associate the warning with.
	 * @param index
	 * 		Local-variable index targeted by the instruction.
	 * @param expected
	 * 		Number of stack values required by the instruction.
	 * @param actual
	 * 		Number of values currently on the operand stack.
	 */
	protected void warnInvalidStoreStack(@NotNull AbstractInsnNode instruction, int index, int expected, int actual) {
		String name = varCache.getVarName(index);
		String local = name == null ? "local " + index : "local '" + name + "'";
		warn(instruction, "Cannot store into " + local + ": expected " + expected
				+ " value" + (expected == 1 ? "" : "s") + " on the operand stack, found " + actual);
	}

	/**
	 * Validate that the input type can be used as the receiver type for the given owner.
	 *
	 * @param instruction
	 * 		Instruction to associate the warning with.
	 * @param inputType
	 * 		Input type to validate.
	 * @param owner
	 * 		Owner type to validate against.
	 * @param operation
	 * 		Action verb to use in the warning message.
	 */
	protected void validateReceiver(@NotNull AbstractInsnNode instruction, @Nullable Type inputType,
	                                @NotNull Type owner, @NotNull String operation) {
		if (inputType == null) {
			warn(instruction, "Cannot " + operation + " through 'null' reference");
			return;
		}
		if (JvmTypeUtils.isUninitialized(inputType)) {
			warn(instruction, "Cannot " + operation + " through uninitialized object");
			return;
		}
		if (!JvmTypeUtils.isReference(inputType)) {
			warn(instruction, "Cannot " + operation + " through non-reference value");
			return;
		}
		if (!isReferenceAssignable(inputType, owner))
			warn(instruction, "Receiver type " + inputType.getDescriptor()
					+ " is not assignable to " + owner.getDescriptor());
	}

	/**
	 * Validate that the input type can be used as the return value for the given expected type.
	 *
	 * @param instruction
	 * 		Instruction to associate the warning with.
	 * @param inputType
	 * 		Input type to validate.
	 * @param expected
	 * 		Expected return type to validate against.
	 */
	protected void validateReturnValue(@NotNull AbstractInsnNode instruction, @Nullable Type inputType,
	                                   @NotNull Type expected) {
		if (JvmTypeUtils.isUninitialized(inputType)) {
			warn(instruction, "Cannot return an uninitialized object");
			return;
		}
		if (expected.equals(JvmTypeUtils.VOID)) {
			if (inputType != null)
				warn(instruction, "Void return requires an empty operand stack");
		} else {
			validateTypeUse(instruction, inputType, expected, "return", "return type");
		}
	}

	/**
	 * Validate that the given frame has an empty operand stack.
	 *
	 * @param instruction
	 * 		Instruction to associate the warning with.
	 * @param frame
	 * 		Frame to validate.
	 */
	protected void validateEmptyStack(@NotNull AbstractInsnNode instruction, @NotNull Frame frame) {
		if (frame instanceof me.darknet.assembler.backend.jvm.compile.analysis.frame.TypedFrame typed && !typed.getStack().isEmpty())
			warn(instruction, "Return instruction leaves values on the operand stack");
		if (frame instanceof me.darknet.assembler.backend.jvm.compile.analysis.frame.ValuedFrame valued && !valued.getStack().isEmpty())
			warn(instruction, "Return instruction leaves values on the operand stack");
	}

	/**
	 * @param inputType
	 * 		Type to check if it can be assigned to the destination type.
	 * @param destinationType
	 * 		Type to check if it can be assigned from the input type.
	 *
	 * @return {@code true} if the input type can be assigned to the destination type, {@code false} otherwise.
	 */
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

	/**
	 * @param inputType
	 * 		Type to check if it can be assigned to the destination type.
	 * @param destinationType
	 * 		Type to check if it can be assigned from the input type.
	 *
	 * @return {@code true} if the input type can be assigned to the destination type, {@code false} otherwise.
	 */
	private boolean isArrayAssignable(@NotNull Type inputType, @NotNull Type destinationType) {
		Type inputComponent = immediateComponentType(inputType);
		Type destinationComponent = immediateComponentType(destinationType);
		if (inputComponent.equals(destinationComponent))
			return true;
		if (JvmTypeUtils.isPrimitive(inputComponent) || JvmTypeUtils.isPrimitive(destinationComponent))
			return false;
		return isReferenceAssignable(inputComponent, destinationComponent);
	}

	/**
	 * @param inputType
	 * 		Type to check if it can be assigned to the destination type.
	 * @param destinationType
	 * 		Type to check if it can be assigned from the input type.
	 *
	 * @return {@code true} if the input type can be assigned to the destination type, {@code false} otherwise.
	 */
	private boolean isAssignableViaChecker(@NotNull Type inputType, @NotNull Type destinationType) {
		return checker != null && checker.isSubclassOf(
				JvmTypeUtils.internalName(inputType),
				JvmTypeUtils.internalName(destinationType)
		);
	}

	/**
	 * @param type
	 * 		Type to check if it is a supertype of an array.
	 *
	 * @return {@code true} if the type is a supertype of an array, {@code false} otherwise.
	 */
	private static boolean isArraySupertype(@NotNull Type type) {
		String internalName = type.getInternalName();
		return "java/lang/Object".equals(internalName) ||
				"java/lang/Cloneable".equals(internalName) ||
				"java/io/Serializable".equals(internalName);
	}

	/**
	 * Take a descriptor like {@code [[I} and transform it to {@code [I}.
	 *
	 * @param arrayType
	 * 		Array type to get the immediate component type of.
	 *
	 * @return Immediate component type of the array type.
	 */
	private static @NotNull Type immediateComponentType(@NotNull Type arrayType) {
		return Type.getType(arrayType.getDescriptor().substring(1));
	}
}
