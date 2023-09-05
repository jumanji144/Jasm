package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.attribute.Parameter;
import dev.xdark.blw.classfile.attribute.generic.GenericParameter;
import dev.xdark.blw.code.generic.GenericCodeBuilder;
import dev.xdark.blw.type.MethodType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.builder.BlwReplaceMethodBuilder;
import me.darknet.assembler.helper.Names;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;

import java.util.ArrayList;
import java.util.List;

public class BlwMethodVisitor extends BlwMemberVisitor implements ASTMethodVisitor {

    private final BlwReplaceMethodBuilder.Nested<?> builder;
    private final List<String> parameterNames = new ArrayList<>();
    private final List<Parameter> parameters = new ArrayList<>();
    private final MethodType type;
    private Names names;

    public BlwMethodVisitor(MethodType type, BlwReplaceMethodBuilder.Nested<?> builder) {
        super(builder);
        this.type = type;
        this.builder = builder;
    }

    @Override
    public void visitParameter(int index, ASTIdentifier name) {
        String literal = name.literal();
        parameters.add(new GenericParameter(0, literal));
        parameterNames.add(literal);
    }

    @Override
    public ASTJvmInstructionVisitor visitJvmCode() {
        return new BlwCodeVisitor(type, (GenericCodeBuilder.Nested<?>) builder.code(), parameterNames);
    }

    @Override
    public void visitEnd() {
        builder.parameters(parameters);
    }
}
