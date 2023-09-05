package me.darknet.assembler.compile.builder;

import dev.xdark.blw.annotation.AnnotationBuilder;
import dev.xdark.blw.annotation.Element;
import dev.xdark.blw.classfile.MemberBuilder;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.MethodBuilder;
import dev.xdark.blw.classfile.attribute.Parameter;
import dev.xdark.blw.classfile.generic.GenericMethod;
import dev.xdark.blw.code.Code;
import dev.xdark.blw.code.CodeBuilder;
import dev.xdark.blw.code.generic.GenericCodeBuilder;
import dev.xdark.blw.internal.BuilderShadow;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.util.Builder;
import dev.xdark.blw.util.Reflectable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlwReplaceMethodBuilder extends BlwReplaceMemberBuilder implements MethodBuilder, BuilderShadow<Method> {

    protected MethodType type;
    protected int accessFlags;
    protected String name;
    protected List<InstanceType> exceptionTypes = List.of();
    private List<Parameter> parameters = List.of();
    private Reflectable<Code> code;
    private Reflectable<? extends Element> annotationDefault;

    @Override
    public MethodBuilder exceptionTypes(List<InstanceType> exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
        return this;
    }

    @Override
    public MethodBuilder parameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public MethodBuilder parameter(Parameter parameter) {
        parameters.add(parameter);
        return this;
    }

    @Override
    public MethodBuilder code(Code code) {
        this.code = Reflectable.wrap(code);
        return this;
    }

    @Override
    public MethodBuilder annotationDefault(Element annotationDefault) {
        this.annotationDefault = Reflectable.wrap(annotationDefault);
        return this;
    }

    @Override
    public MethodBuilder annotationDefault(Reflectable<? extends Element> annotationDefault) {
        this.annotationDefault = annotationDefault;
        return this;
    }

    @Override
    public CodeBuilder.Nested<? extends MethodBuilder> code() {
        Reflectable<Code> code = this.code;
        if (!(code instanceof CodeBuilder)) {
            code = new GenericCodeBuilder.Nested<>((Builder) this);
            this.code = code;
        }
        //noinspection unchecked
        return (CodeBuilder.Nested<? extends MethodBuilder>) code;
    }

    @Override
    public MemberBuilder signature(@Nullable String signature) {
        return super.signature(signature);
    }

    @Override
    public AnnotationBuilder.@Nullable Nested<? extends MemberBuilder> visibleRuntimeAnnotation(InstanceType type) {
        return super.visibleRuntimeAnnotation(type);
    }

    @Override
    public AnnotationBuilder.@Nullable Nested<? extends MemberBuilder> invisibleRuntimeAnnotation(InstanceType type) {
        return super.invisibleRuntimeAnnotation(type);
    }

    @Override
    public Method build() {
        Reflectable<Code> code;
        Reflectable<? extends Element> annotationDefault;
        return new GenericMethod(
                accessFlags,
                name,
                signature,
                visibleRuntimeAnnotations(),
                invisibleRuntimeAnnotation(),
                type,
                (code = this.code) == null ? null : code.reflectAs(),
                exceptionTypes,
                parameters,
                (annotationDefault = this.annotationDefault) == null ? null : annotationDefault.reflectAs()
        );
    }

    public static final class Root extends BlwReplaceMethodBuilder implements MethodBuilder.Root {

        @Override
        public MethodBuilder.Root type(MethodType type) {
            return this;
        }

        @Override
        public MethodBuilder.Root accessFlags(int accessFlags) {
            return this;
        }

        @Override
        public MethodBuilder.Root name(String name) {
            return this;
        }

        @Override
        public MethodBuilder.Root exceptionTypes(List<InstanceType> exceptionTypes) {
            return (MethodBuilder.Root) super.exceptionTypes(exceptionTypes);
        }

        @Override
        public MethodBuilder.Root parameters(List<Parameter> parameters) {
            return (MethodBuilder.Root) super.parameters(parameters);
        }

        @Override
        public MethodBuilder.Root parameter(Parameter parameter) {
            return (MethodBuilder.Root) super.parameter(parameter);
        }

        @Override
        public MethodBuilder.Root signature(@Nullable String signature) {
            return (MethodBuilder.Root) super.signature(signature);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<MethodBuilder.Root> visibleRuntimeAnnotation(InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<MethodBuilder.Root>) super.visibleRuntimeAnnotation(type);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<MethodBuilder.Root> invisibleRuntimeAnnotation(InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<MethodBuilder.Root>) super.invisibleRuntimeAnnotation(type);
        }

        @Override
        public CodeBuilder.Nested<MethodBuilder.Root> code() {
            //noinspection unchecked
            return (CodeBuilder.Nested<MethodBuilder.Root>) super.code();
        }

        @Override
        public MethodBuilder.Root annotationDefault(Element annotationDefault) {
            return (MethodBuilder.Root) super.annotationDefault(annotationDefault);
        }

        @Override
        public MethodBuilder.Root annotationDefault(Reflectable<? extends Element> annotationDefault) {
            return (MethodBuilder.Root) super.annotationDefault(annotationDefault);
        }

        @Override
        public Method reflectAs() {
            return super.reflectAs();
        }
    }

    public static final class Nested<U extends Builder> extends BlwReplaceMethodBuilder implements MethodBuilder.Nested<U> {
        private final U upstream;

        public Nested(int accessFlags, String name, MethodType type, U upstream) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.type = type;
            this.upstream = upstream;
        }

        @Override
        public MethodBuilder.Nested<U> parameters(List<Parameter> parameters) {
            //noinspection unchecked
            return (MethodBuilder.Nested<U>) super.parameters(parameters);
        }

        @Override
        public MethodBuilder.Nested<U> parameter(Parameter parameter) {
            //noinspection unchecked
            return (MethodBuilder.Nested<U>) super.parameter(parameter);
        }

        @Override
        public CodeBuilder.Nested<MethodBuilder.Nested<U>> code() {
            //noinspection unchecked
            return (CodeBuilder.Nested<MethodBuilder.Nested<U>>) super.code();
        }

        @Override
        public MethodBuilder.Nested<U> signature(@Nullable String signature) {
            //noinspection unchecked
            return (MethodBuilder.Nested<U>) super.signature(signature);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<MethodBuilder.Nested<U>> visibleRuntimeAnnotation(InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<MethodBuilder.Nested<U>>) super.visibleRuntimeAnnotation(type);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<MethodBuilder.Nested<U>> invisibleRuntimeAnnotation(InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<MethodBuilder.Nested<U>>) super.invisibleRuntimeAnnotation(type);
        }

        @Override
        public MethodBuilder.Nested<U> annotationDefault(Element annotationDefault) {
            //noinspection unchecked
            return (MethodBuilder.Nested<U>) super.annotationDefault(annotationDefault);
        }

        @Override
        public MethodBuilder.Nested<U> annotationDefault(Reflectable<? extends Element> annotationDefault) {
            //noinspection unchecked
            return (MethodBuilder.Nested<U>) super.annotationDefault(annotationDefault);
        }

        @Override
        public U __() {
            return upstream;
        }
    }
}
