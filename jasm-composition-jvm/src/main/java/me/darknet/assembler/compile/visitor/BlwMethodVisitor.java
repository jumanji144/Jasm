package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.attribute.Parameter;
import dev.xdark.blw.classfile.attribute.generic.GenericParameter;
import dev.xdark.blw.code.generic.GenericCodeBuilder;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.ObjectType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.builder.BlwReplaceMethodBuilder;
import me.darknet.assembler.helper.Names;
import me.darknet.assembler.util.TypedVariable;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;

import java.util.ArrayList;
import java.util.List;

public class BlwMethodVisitor extends BlwMemberVisitor implements ASTMethodVisitor {

    private final BlwReplaceMethodBuilder.Nested<?> builder;
    private final List<String> parameterNames = new ArrayList<>();
    private final List<Parameter> parameters = new ArrayList<>();
    private final MethodType type;
    private final ObjectType owner;
    private final boolean isStatic;
    private Names names;

    public BlwMethodVisitor(ObjectType owner, MethodType type, boolean isStatic,
                            BlwReplaceMethodBuilder.Nested<?> builder) {
        super(builder);
        this.type = type;
        this.owner = owner;
        this.isStatic = isStatic;
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
        List<TypedVariable> parameters = new ArrayList<>();

        if(!isStatic) {
            parameters.add(new TypedVariable("this", owner));
        }

        for (int i = 0; i < type.parameterTypes().size(); i++) {
            String name;
            if(i < parameterNames.size()) {
                name = parameterNames.get(i);
            } else {
                name = "p" + i;
            }
            parameters.add(new TypedVariable(name, type.parameterTypes().get(i)));
        }

        return new BlwCodeVisitor((GenericCodeBuilder.Nested<?>) builder.code(), parameters);
    }

    @Override
    public void visitEnd() {
        builder.parameters(parameters);
    }
}
