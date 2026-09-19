package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AST model of a method declaration.
 */
public class ASTMethod extends ASTMember {

    private final List<ASTIdentifier> parameters;
    private final Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations;
    private final List<ASTIdentifier> declaredExceptions;
    private final List<ASTException> exceptions;
    private final ASTElement defaultValue;
    private final ASTCode code;
    private final List<Instruction<?>> instructions;
    private final BytecodeFormat format;
    private @Nullable ASTNumber registerCount;

    public ASTMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor, List<ASTIdentifier> parameters,
                     Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations,
                     List<ASTIdentifier> declaredExceptions, ASTElement defaultValue, List<ASTException> exceptions, ASTCode code,
                     List<Instruction<?>> instructions, BytecodeFormat format) {
        super(ElementType.METHOD, modifiers, name, descriptor);
        this.parameters = CollectionUtil.immutableCopy(parameters);
        this.parameterAnnotations = freezeParameterAnnotations(parameterAnnotations);
        this.declaredExceptions = CollectionUtil.immutableCopy(declaredExceptions);
        this.exceptions = CollectionUtil.immutableCopy(exceptions);
        this.defaultValue = defaultValue;
        this.code = code;
        this.instructions = CollectionUtil.immutableCopy(instructions);
        this.format = format;
        addChildren(this.parameters);
        addChildren(this.declaredExceptions);
        addChildren(this.exceptions);
        for (List<ASTAnnotation> annotations : this.parameterAnnotations.values()) {
            addChildren(annotations);
        }
        if (defaultValue != null) addChild(defaultValue);
        if (code != null) addChild(code);
    }

    /**
     * @return Default annotation value for this method, or {@code null} when not present.
     */
    public @Nullable ASTElement getAnnotationDefaultValue() {
        return defaultValue;
    }

    /**
     * @return List of parameter names for this method.
     */
    public List<ASTIdentifier> getParameters() {
        return parameters;
    }

    /**
     * @return Map of parameter names to their annotations.
     */
    public Map<ASTIdentifier, List<ASTAnnotation>> getParameterAnnotations() {
        return parameterAnnotations;
    }

    /**
     * @return List of thrown exceptions declared by this method.
     */
    public List<ASTIdentifier> getDeclaredExceptions() {
        return declaredExceptions;
    }

    /**
     * @return List of try-catch handlers for this method.
     */
    public List<ASTException> getExceptionHandlers() {
        return exceptions;
    }

    /**
     * @return AST code model for this method, or {@code null} when the method is abstract or native.
     */
    public ASTCode getCode() {
        return code;
    }

    /**
     * @return Explicit Dalvik register count, or {@code null} for non-Dalvik methods or when omitted.
     */
    public @Nullable ASTNumber getRegisterCount() {
        return registerCount;
    }

	/**
	 * @param registerCount
	 * 		Explicit Dalvik register count, or {@code null} for non-Dalvik methods or when omitted.
	 */
	public void setRegisterCount(@Nullable ASTNumber registerCount) {
		replaceChild(this.registerCount, registerCount);
		this.registerCount = registerCount;
	}

    @SuppressWarnings("UnnecessaryLocalVariable")
    public void accept(ErrorCollector collector, ASTMethodVisitor visitor) {
        super.accept(collector, visitor);

        if (registerCount != null)
            visitor.visitRegisterCount(registerCount.asInt());

        List<ASTIdentifier> localParams = parameters;
        for (int i = 0; i < localParams.size(); i++)
            visitor.visitParameter(i, localParams.get(i));

        for (ASTIdentifier declaredException : declaredExceptions)
            visitor.visitDeclaredException(declaredException);

        parameterAnnotations.forEach((id, annos) -> {
            int sourceParameterIndex = findSourceParameterIndex(id.content());
            int jvmParameterIndex = sourceParameterIndexToJvmParameterIndex(sourceParameterIndex);
            if (jvmParameterIndex < 0)
                return;
            for (ASTAnnotation annotation : annos) {
                if (annotation.isVisible())
                    annotation.accept(collector, visitor.visitVisibleParameterAnnotation(jvmParameterIndex, annotation.getClassType()));
                else
                    annotation.accept(collector, visitor.visitInvisibleParameterAnnotation(jvmParameterIndex, annotation.getClassType()));
            }
        });

        if (defaultValue != null)
            visitor.visitAnnotationDefaultValue(defaultValue);

        if (code == null) {
            visitor.visitEnd();
            return;
        }

        ASTInstructionVisitor instructionVisitor = switch (format) {
            case JVM -> visitor.visitJvmCode(collector);
            case DALVIK -> visitor.visitDalvikCode(collector);
        };
        if (instructionVisitor != null) {
            int instructionIndex = 0;
            List<ASTInstruction> localAstInstructions = code.getInstructions();
            List<Instruction<?>> localIrInstructions = instructions;
            for (ASTInstruction instruction : localAstInstructions) {
                instructionVisitor.visitInstruction(instruction);
                if (instruction instanceof ASTLabel lab) {
                    instructionVisitor.visitLabel(lab.identifier());
                } else {
                    localIrInstructions.get(instructionIndex++).transform(instruction, instructionVisitor);
                }
            }

            for (ASTException exception : exceptions) {
                instructionVisitor.visitException(
                        exception.start(), exception.end(), exception.handler(), exception.exceptionType()
                );
            }

            instructionVisitor.visitEnd();
        }

        visitor.visitEnd();
    }

    protected int findSourceParameterIndex(String name) {
        for (int i = 0; i < parameters.size(); i++) {
            ASTIdentifier parameter = parameters.get(i);
            if (Objects.equals(name, parameter.content()))
                return i;
        }
        return -1;
    }

    private boolean hasReceiverParameter() {
        return !getModifiers().hasModifier("static");
    }

    private int sourceParameterIndexToJvmParameterIndex(int sourceParameterIndex) {
        if (sourceParameterIndex < 0)
            return -1;
        if (hasReceiverParameter() && sourceParameterIndex == 0)
            return -1;
        return sourceParameterIndex - (hasReceiverParameter() ? 1 : 0);
    }

    private static Map<ASTIdentifier, List<ASTAnnotation>> freezeParameterAnnotations(
            Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations) {
        if (parameterAnnotations == null || parameterAnnotations.isEmpty())
            return Collections.emptyMap();
        Map<ASTIdentifier, List<ASTAnnotation>> copy = new IdentityHashMap<>();
        parameterAnnotations.forEach((parameter, annotations) ->
                copy.put(parameter, CollectionUtil.immutableCopy(annotations)));
        return Collections.unmodifiableMap(copy);
    }
}
