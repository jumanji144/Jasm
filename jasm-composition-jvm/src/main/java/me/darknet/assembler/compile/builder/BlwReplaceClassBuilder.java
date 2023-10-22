package me.darknet.assembler.compile.builder;

import dev.xdark.blw.annotation.Annotation;
import dev.xdark.blw.annotation.AnnotationBuilder;
import dev.xdark.blw.annotation.generic.GenericAnnotationBuilder;
import dev.xdark.blw.classfile.*;
import dev.xdark.blw.classfile.attribute.InnerClass;
import dev.xdark.blw.classfile.generic.GenericClassFileView;
import dev.xdark.blw.constantpool.ConstantPool;
import dev.xdark.blw.internal.BuilderShadow;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.version.JavaVersion;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlwReplaceClassBuilder implements ClassBuilder {

    private final Map<Integer, BuilderShadow<Annotation>> visibleRuntimeAnnotations = new LinkedHashMap<>();
    private final Map<Integer, BuilderShadow<Annotation>> invisibleRuntimeAnnotations = new LinkedHashMap<>();
    private final Map<String, BlwReplaceMethodBuilder> methods = new LinkedHashMap<>();
    private final Map<String, BlwReplaceFieldBuilder> fields = new LinkedHashMap<>();
    private List<InnerClass> innerClasses = List.of();
    private InstanceType nestHost;
    private String sourceFile, sourceDebug;
    private int accessFlags;
    private String signature;
    private ConstantPool pool;
    private InstanceType type;
    private InstanceType superClass;
    private List<InstanceType> interfaces = List.of();
    private JavaVersion version;

    public InstanceType getType() {
        return type;
    }

    public Map<String, BlwReplaceFieldBuilder> getFields() {
        return fields;
    }

    public Map<String, BlwReplaceMethodBuilder> getMethods() {
        return methods;
    }

    @Override
    public ClassBuilder accessFlags(int accessFlags) {
        this.accessFlags = accessFlags;
        return this;
    }

    @Override
    public ClassBuilder signature(@Nullable String signature) {
        this.signature = signature;
        return this;
    }

    @Override
    public AnnotationBuilder.Nested<ClassBuilder> visibleRuntimeAnnotation(InstanceType type) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (ClassBuilder) this);
        visibleRuntimeAnnotations.put(visibleRuntimeAnnotations.size(), builder);
        return builder;
    }

    @Override
    public AnnotationBuilder.Nested<ClassBuilder> invisibleRuntimeAnnotation(InstanceType type) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (ClassBuilder) this);
        invisibleRuntimeAnnotations.put(invisibleRuntimeAnnotations.size(), builder);
        return builder;
    }

    public AnnotationBuilder.Nested<ClassBuilder> visibleRuntimeAnnotation(InstanceType type, int index) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (ClassBuilder) this);
        visibleRuntimeAnnotations.put(index, builder);
        return builder;
    }

    public AnnotationBuilder.Nested<ClassBuilder> invisibleRuntimeAnnotation(InstanceType type, int index) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (ClassBuilder) this);
        invisibleRuntimeAnnotations.put(index, builder);
        return builder;
    }

    @Override
    public ClassBuilder constantPool(@Nullable ConstantPool constantPool) {
        pool = constantPool;
        return this;
    }

    @Override
    public ClassBuilder type(InstanceType type) {
        this.type = type;
        return this;
    }

    @Override
    public ClassBuilder superClass(@Nullable InstanceType superClass) {
        this.superClass = superClass;
        return this;
    }

    @Override
    public ClassBuilder interfaces(List<InstanceType> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    @Override
    public ClassBuilder version(JavaVersion version) {
        this.version = version;
        return this;
    }

    @Override
    public BlwReplaceMethodBuilder.Nested<ClassBuilder> method(int accessFlags, String name, MethodType type) {
        var builder = new BlwReplaceMethodBuilder.Nested<ClassBuilder>(accessFlags, name, type, this);
        methods.put(name + type.descriptor(), builder);
        return builder;
    }

    @Override
    public ClassBuilder method(MethodBuilder.Root method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassBuilder method(Method method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlwReplaceFieldBuilder.Nested<ClassBuilder> field(int accessFlags, String name, ClassType type) {
        var builder = new BlwReplaceFieldBuilder.Nested<ClassBuilder>(accessFlags, name, type, this);
        fields.put(name + type.descriptor(), builder);
        return builder;
    }

    @Override
    public ClassBuilder field(FieldBuilder.Root field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassBuilder field(Field field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassBuilder innerClasses(List<InnerClass> innerClasses) {
        this.innerClasses = innerClasses;
        return this;
    }

    @Override
    public ClassBuilder innerClass(InnerClass innerClass) {
        List<InnerClass> innerClasses = this.innerClasses;
        if (innerClasses.isEmpty()) {
            innerClasses = new ArrayList<>();
            this.innerClasses = innerClasses;
        }
        innerClasses.add(innerClass);
        return this;
    }

    @Override
    public ClassBuilder nestHost(@Nullable InstanceType nestHost) {
        this.nestHost = nestHost;
        return this;
    }

    @Override
    public ClassBuilder sourceFile(@Nullable String sourceFile) {
        this.sourceFile = sourceFile;
        return this;
    }

    @Override
    public ClassBuilder sourceDebug(@Nullable String sourceDebug) {
        this.sourceDebug = sourceDebug;
        return this;
    }

    @Override
    public ClassFileView build() {
        return new GenericClassFileView(
                version, pool, accessFlags, type, superClass, signature, interfaces, buildList(methods.values()),
                buildList(fields.values()), innerClasses, nestHost, sourceFile, sourceDebug,
                buildList(visibleRuntimeAnnotations.values()), buildList(invisibleRuntimeAnnotations.values())
        );
    }

    private static <T> List<T> buildList(Collection<? extends BuilderShadow<T>> builders) {
        return builders.stream().map(BuilderShadow::build).toList();
    }

}
