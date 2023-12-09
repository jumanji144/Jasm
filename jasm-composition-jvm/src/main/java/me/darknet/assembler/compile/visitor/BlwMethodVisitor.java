package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.builder.BlwReplaceMethodBuilder;
import me.darknet.assembler.util.CastUtil;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;

import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.attribute.Parameter;
import dev.xdark.blw.classfile.attribute.generic.GenericParameter;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BlwMethodVisitor extends BlwMemberVisitor<MethodType, Method> implements ASTMethodVisitor {
    private final BlwReplaceMethodBuilder builder;
    private final JvmCompilerOptions options;
    private final Consumer<AnalysisResults> analysisResultsConsumer;
    private final List<String> parameterNames = new ArrayList<>();
    private final List<Parameter> parameters = new ArrayList<>();
    private final MethodType type;
    private final ObjectType owner;
    private final boolean isStatic;

    public BlwMethodVisitor(JvmCompilerOptions options, ObjectType owner, MethodType type, boolean isStatic,
            BlwReplaceMethodBuilder builder, Consumer<AnalysisResults> analysisResultsConsumer) {
        super(CastUtil.cast(builder));
        this.options = options;
        this.type = type;
        this.owner = owner;
        this.isStatic = isStatic;
        this.builder = builder;
        this.analysisResultsConsumer = analysisResultsConsumer;
    }

    @Override
    public void visitParameter(int index, ASTIdentifier name) {
        String literal = name.literal();
        parameters.add(new GenericParameter(0, literal));
        parameterNames.add(literal);
    }

    @Override
    public ASTJvmInstructionVisitor visitJvmCode() {
        List<Local> parameters = new ArrayList<>();

        int localIndex = 0;
        if (!isStatic) {
            parameters.add(new Local(localIndex++, "this", owner));
        }

        for (int i = 0; i < type.parameterTypes().size(); i++) {
            String name;
            if (i < parameterNames.size()) {
                name = parameterNames.get(i + (isStatic ? 0 : 1));
            } else {
                name = "p" + i;
            }
            ClassType type = this.type.parameterTypes().get(i);
            parameters.add(new Local(localIndex++, name, type));
            if (type == Types.LONG || type == Types.DOUBLE) {
                parameters.add(null);
                localIndex++;
            }
        }

        return new BlwCodeVisitor(options, builder.code().child(), parameters) {
            @Override
            public void visitEnd() {
                super.visitEnd();

                // The parent impl of 'visitEnd' populates the method's stack frame analysis,
                // so our extension here will pass it along to the consumer of this visitor, if one exists.
                if (analysisResultsConsumer != null)
                    analysisResultsConsumer.accept(getAnalysisResults());
            }
        };
    }

    @Override
    public void visitEnd() {
        builder.parameters(parameters);
    }
}
