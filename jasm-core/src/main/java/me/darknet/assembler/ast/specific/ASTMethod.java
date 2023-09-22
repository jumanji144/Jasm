package me.darknet.assembler.ast.specific;

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

public class ASTMethod extends ASTMember {

    private final ASTIdentifier descriptor;
    private final List<ASTIdentifier> parameters;
    private final ASTCode code;
    private final List<Instruction<?>> instructions;
    private final BytecodeFormat format;

    public ASTMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor,
            @Nullable ASTIdentifier signature, @Nullable List<ASTAnnotation> annotations,
            List<ASTIdentifier> parameters, ASTCode code, List<Instruction<?>> instructions, BytecodeFormat format) {
        super(ElementType.METHOD, modifiers, name, signature, annotations);
        this.descriptor = descriptor;
        this.parameters = parameters;
        this.code = code;
        this.instructions = instructions;
        this.format = format;
    }

    public ASTIdentifier descriptor() {
        return descriptor;
    }

    public List<ASTIdentifier> parameters() {
        return parameters;
    }

    public ASTCode code() {
        return code;
    }

    public List<Instruction<?>> instructions() {
        return instructions;
    }

    public void accept(ErrorCollector collector, ASTMethodVisitor visitor) {
        super.accept(collector, visitor);
        for (int i = 0; i < parameters.size(); i++) {
            visitor.visitParameter(i, parameters.get(i));
        }
        if(this.code == null) {
            visitor.visitEnd();
            return;
        }
        ASTInstructionVisitor instructionVisitor = switch (format) {
            case JVM -> visitor.visitJvmCode();
            case DALVIK -> null;
        };
        if (instructionVisitor != null) {
            int instructionIndex = 0;
            for (int i = 0; i < code.instructions().size(); i++) {
                ASTInstruction instruction = code.instructions().get(i);
                if(instruction instanceof ASTLabel lab) {
                    instructionVisitor.visitLabel(lab.identifier());
                } else {
                    instructionVisitor.visitInstruction(instruction);
                    instructions.get(instructionIndex++).transform(instruction, instructionVisitor);
                }
            }

            instructionVisitor.visitEnd();
        }

        visitor.visitEnd();
    }
}
