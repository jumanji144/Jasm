package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.annotation.Element;
import dev.xdark.blw.annotation.generic.GenericAnnotationBuilder;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.builder.BlwReplaceMethodBuilder;
import me.darknet.assembler.error.ErrorCollectionException;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.CastUtil;
import me.darknet.assembler.util.Pair;
import me.darknet.assembler.util.VarNaming;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;

import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.attribute.Parameter;
import dev.xdark.blw.classfile.attribute.generic.GenericParameter;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BlwMethodVisitor extends BlwMemberVisitor<MethodType, Method> implements BlwElementAdapter, ASTMethodVisitor {
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
    public void visitAnnotationDefaultValue(ASTElement defaultValue) {
        // Create a dummy annotation visitor and extract the first element to get our value to pass along
        GenericAnnotationBuilder elementBuilder = new GenericAnnotationBuilder(Types.OBJECT);
        ErrorCollector collector = new ErrorCollector();
        ASTAnnotationVisitor.accept(new BlwAnnotationVisitor(elementBuilder),
                Collections.singleton(new Pair<>(ASTIdentifier.STUB, defaultValue)),
                collector);
        if (collector.hasErr())
            throw new ErrorCollectionException("Failed building array element from ast", collector);
        Element element = elementBuilder.elements().values().iterator().next().reflectAs();
        builder.annotationDefault(element);
    }

    @Override
    public ASTJvmInstructionVisitor visitJvmCode(@NotNull ErrorCollector collector) {
        List<Local> parameters = new ArrayList<>();

        int localIndex = 0;
        if (!isStatic) {
            parameters.add(new Local(localIndex++, "this", owner));
        }

        for (int i = 0; i < type.parameterTypes().size(); i++) {
            ClassType type = this.type.parameterTypes().get(i);

            String name;
            int nameIndex = i + (isStatic ? 0 : 1);
            if (nameIndex < parameterNames.size())
                name = parameterNames.get(nameIndex);
            else
                name = VarNaming.name(i, type);

            parameters.add(new Local(localIndex++, name, type));
            if (type == Types.LONG || type == Types.DOUBLE) {
                parameters.add(null);
                localIndex++;
            }
        }

        return new BlwCodeVisitor(options, collector, builder.code().child(), type, parameters) {
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
    public ASTAnnotationVisitor visitVisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
        return new BlwAnnotationVisitor(
                builder.addVisibleRuntimeParameterAnnotations(index, Types.instanceTypeFromInternalName(classType.literal())).child()
        );
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
        return new BlwAnnotationVisitor(
                builder.addInvisibleRuntimeParameterAnnotations(index, Types.instanceTypeFromInternalName(classType.literal())).child()
        );
    }

    @Override
    public void visitEnd() {
        builder.parameters(parameters);
    }
}
