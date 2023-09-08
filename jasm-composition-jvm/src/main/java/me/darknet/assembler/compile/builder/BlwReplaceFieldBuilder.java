package me.darknet.assembler.compile.builder;

import dev.xdark.blw.annotation.AnnotationBuilder;
import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.classfile.FieldBuilder;
import dev.xdark.blw.classfile.generic.GenericField;
import dev.xdark.blw.constant.Constant;
import dev.xdark.blw.internal.BuilderShadow;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.util.Builder;
import org.jetbrains.annotations.Nullable;

public class BlwReplaceFieldBuilder extends BlwReplaceMemberBuilder implements BuilderShadow<Field> {

    protected ClassType type;
    protected int accessFlags;
    protected String name;
    protected Constant defaultValue;

    public FieldBuilder defaultValue(@Nullable Constant value) {
        defaultValue = value;
        return (FieldBuilder) this;
    }

    @Override
    public FieldBuilder signature(@Nullable String signature) {
        return (FieldBuilder) super.signature(signature);
    }

    @Override
    public @Nullable AnnotationBuilder.Nested<? extends FieldBuilder> visibleRuntimeAnnotation(InstanceType type) {
        //noinspection unchecked
        return (AnnotationBuilder.Nested<? extends FieldBuilder>) super.visibleRuntimeAnnotation(type);
    }

    @Override
    public @Nullable AnnotationBuilder.Nested<? extends FieldBuilder> invisibleRuntimeAnnotation(InstanceType type) {
        //noinspection unchecked
        return (AnnotationBuilder.Nested<? extends FieldBuilder>) super.invisibleRuntimeAnnotation(type);
    }

    @Override
    public final Field build() {
        return new GenericField(
                accessFlags, name, signature, visibleRuntimeAnnotations(), invisibleRuntimeAnnotation(), type,
                defaultValue
        );
    }

    public static final class Root extends BlwReplaceFieldBuilder implements FieldBuilder.Root {

        @Override
        public FieldBuilder.Root type(ClassType type) {
            this.type = type;
            return this;
        }

        @Override
        public FieldBuilder.Root accessFlags(int accessFlags) {
            this.accessFlags = accessFlags;
            return this;
        }

        @Override
        public FieldBuilder.Root name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public FieldBuilder.Root defaultValue(@Nullable Constant value) {
            return (FieldBuilder.Root) super.defaultValue(value);
        }

        @Override
        public FieldBuilder.Root signature(@Nullable String signature) {
            return (FieldBuilder.Root) super.signature(signature);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<FieldBuilder.Root> visibleRuntimeAnnotation(InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<FieldBuilder.Root>) super.visibleRuntimeAnnotation(type);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<FieldBuilder.Root> invisibleRuntimeAnnotation(InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<FieldBuilder.Root>) super.invisibleRuntimeAnnotation(type);
        }

        @Override
        public Field reflectAs() {
            return super.reflectAs();
        }
    }

    public static final class Nested<U extends Builder> extends BlwReplaceFieldBuilder
            implements FieldBuilder.Nested<U> {
        private final U upstream;

        public Nested(int accessFlags, String name, ClassType type, U upstream) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.type = type;
            this.upstream = upstream;
        }

        @Override
        public FieldBuilder.Nested<U> defaultValue(@Nullable Constant value) {
            //noinspection unchecked
            return (FieldBuilder.Nested<U>) super.defaultValue(value);
        }

        @Override
        public FieldBuilder.Nested<U> signature(@Nullable String signature) {
            //noinspection unchecked
            return (FieldBuilder.Nested<U>) super.signature(signature);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<FieldBuilder.Nested<U>> visibleRuntimeAnnotation(InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<FieldBuilder.Nested<U>>) super.visibleRuntimeAnnotation(type);
        }

        @Override
        public @Nullable AnnotationBuilder.Nested<FieldBuilder.Nested<U>> invisibleRuntimeAnnotation(
                InstanceType type) {
            //noinspection unchecked
            return (AnnotationBuilder.Nested<FieldBuilder.Nested<U>>) super.invisibleRuntimeAnnotation(type);
        }

        @Override
        public U __() {
            return upstream;
        }
    }

}
