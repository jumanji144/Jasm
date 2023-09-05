package me.darknet.assembler.compile.builder;

import dev.xdark.blw.annotation.Annotation;
import dev.xdark.blw.annotation.AnnotationBuilder;
import dev.xdark.blw.annotation.generic.GenericAnnotationBuilder;
import dev.xdark.blw.classfile.MemberBuilder;
import dev.xdark.blw.internal.BuilderShadow;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.util.Builder;
import dev.xdark.blw.util.Reflectable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BlwReplaceMemberBuilder implements MemberBuilder {
    private final Map<String, Reflectable<Annotation>> visibleRuntimeAnnotations = new HashMap<>();
    private final Map<String, Reflectable<Annotation>> invisibleRuntimeAnnotations = new HashMap<>();
    protected String signature;

    @Override
    public MemberBuilder signature(@Nullable String signature) {
        this.signature = signature;
        return this;
    }

    @Override
    public @Nullable AnnotationBuilder.Nested<? extends MemberBuilder> visibleRuntimeAnnotation(InstanceType type) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (Builder) this);
        visibleRuntimeAnnotations.put(type.descriptor() + visibleRuntimeAnnotations.size(), builder);
        //noinspection unchecked
        return (AnnotationBuilder.Nested) builder;
    }

    @Override
    public @Nullable AnnotationBuilder.Nested<? extends MemberBuilder> invisibleRuntimeAnnotation(InstanceType type) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (Builder) this);
        invisibleRuntimeAnnotations.put(type.descriptor() + invisibleRuntimeAnnotations.size(), builder);
        //noinspection unchecked
        return (AnnotationBuilder.Nested) builder;
    }

    public @Nullable AnnotationBuilder.Nested<? extends MemberBuilder> visibleRuntimeAnnotation(InstanceType type,
                                                                                                int index) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (Builder) this);
        visibleRuntimeAnnotations.put(type.descriptor() + index, builder);
        //noinspection unchecked
        return (AnnotationBuilder.Nested) builder;
    }

    public @Nullable AnnotationBuilder.Nested<? extends MemberBuilder> invisibleRuntimeAnnotation(InstanceType type,
                                                                                                  int index) {
        var builder = new GenericAnnotationBuilder.Nested<>(type, (Builder) this);
        invisibleRuntimeAnnotations.put(type.descriptor() + index, builder);
        //noinspection unchecked
        return (AnnotationBuilder.Nested) builder;
    }


    protected final List<Annotation> visibleRuntimeAnnotations() {
        return visibleRuntimeAnnotations.values().stream().map(builder -> ((BuilderShadow<Annotation>) builder).build()).toList();
    }

    protected final List<Annotation> invisibleRuntimeAnnotation() {
        return invisibleRuntimeAnnotations.values().stream().map(builder -> ((BuilderShadow<Annotation>) builder).build()).toList();
    }
}