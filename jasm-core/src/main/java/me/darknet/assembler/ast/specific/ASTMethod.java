package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTMethod extends ASTMember {

    private final ASTIdentifier descriptor;
    private final List<ASTIdentifier> parameters;
    private final ASTCode code;

    public ASTMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor,
            @Nullable ASTIdentifier signature, @Nullable List<ASTAnnotation> annotations,
            List<ASTIdentifier> parameters, ASTCode code) {
        super(ElementType.METHOD, modifiers, name, signature, annotations);
        this.descriptor = descriptor;
        this.parameters = parameters;
        this.code = code;
    }

    public ASTIdentifier getDescriptor() {
        return descriptor;
    }

    public List<ASTIdentifier> getParameters() {
        return parameters;
    }

    public ASTCode getCode() {
        return code;
    }

}
