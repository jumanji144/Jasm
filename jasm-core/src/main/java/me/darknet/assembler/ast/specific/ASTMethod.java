package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ASTMethod extends ASTMember {

    private final List<ASTIdentifier> parameters;
    private final Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations;
    private final List<ASTException> exceptions;
    private final ASTElement defaultValue;
    private final ASTCode code;
    private final List<Instruction<?>> instructions;
    private final BytecodeFormat format;

    public ASTMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor, List<ASTIdentifier> parameters,
                     Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations,
                     ASTElement defaultValue, List<ASTException> exceptions, ASTCode code,
                     List<Instruction<?>> instructions, BytecodeFormat format) {
        super(ElementType.METHOD, modifiers, name, descriptor);
        this.parameters = parameters;
        this.parameterAnnotations = parameterAnnotations;
        this.exceptions = exceptions;
        this.defaultValue = defaultValue;
        this.code = code;
        this.instructions = instructions;
        this.format = format;
        addChildren(parameters);
        addChildren(exceptions);
        if (defaultValue != null) addChild(defaultValue);
        if (code != null) addChild(code);
    }

    public @Nullable ASTElement getAnnotationDefaultValue() {
        return defaultValue;
    }

    public List<ASTIdentifier> parameters() {
        return parameters;
    }

    public Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations() {
        return parameterAnnotations;
    }

    public List<ASTException> exceptions() {
        return exceptions;
    }

    public ASTCode code() {
        return code;
    }

    public List<Instruction<?>> instructions() {
        return instructions;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public void accept(ErrorCollector collector, ASTMethodVisitor visitor) {
        super.accept(collector, visitor);
        List<ASTIdentifier> localParams = parameters;
        for (int i = 0; i < localParams.size(); i++) {
            visitor.visitParameter(i, localParams.get(i));
        }
        parameterAnnotations.forEach((id, annos) -> {
            int parameterIndex = findParameterIndex(id.content());

            // We artificially bumped the parameter indices by one earlier when adding "this" as a parameter
            // and now need to bump the index back down when writing the annotation back.
            if (!getModifiers().hasModifier("static"))
                parameterIndex--;

            if (parameterIndex < 0)
                return;
            for (ASTAnnotation annotation : annos) {
                if (annotation.isVisible())
                    annotation.accept(collector, visitor.visitVisibleParameterAnnotation(parameterIndex, annotation.classType()));
                else
                    annotation.accept(collector, visitor.visitInvisibleParameterAnnotation(parameterIndex, annotation.classType()));
            }
        });
        if (this.defaultValue != null) {
            visitor.visitAnnotationDefaultValue(defaultValue);
        }
        if (this.code == null) {
            visitor.visitEnd();
            return;
        }

        ASTInstructionVisitor instructionVisitor = switch (format) {
            case JVM -> visitor.visitJvmCode(collector);
            case DALVIK -> null;
        };
        if (instructionVisitor != null) {
            int instructionIndex = 0;
            List<ASTInstruction> localAstInstructions = code.instructions();
            List<Instruction<?>> localIrInstructions = instructions;
            for (ASTInstruction instruction : localAstInstructions) {
                if (instruction instanceof ASTLabel lab) {
                    instructionVisitor.visitLabel(lab.identifier());
                } else {
                    if (!instruction.identifier().content().equals("line"))
                        instructionVisitor.visitInstruction(instruction);
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

    protected int findParameterIndex(String name) {
        for (int i = 0; i < parameters.size(); i++) {
            ASTIdentifier parameter = parameters.get(i);
            if (parameter.content().equals(name))
                return i;
        }
        return -1;
    }
}
