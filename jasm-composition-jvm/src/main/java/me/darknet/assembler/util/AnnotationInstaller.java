package me.darknet.assembler.util;

import dev.xdark.blw.annotation.AnnotationBuilder;
import dev.xdark.blw.classfile.AnnotatedBuilder;
import dev.xdark.blw.type.InstanceType;
import org.jetbrains.annotations.NotNull;

public class AnnotationInstaller {
    public static <A extends AnnotationBuilder<A>> @NotNull A install(@NotNull AnnotatedBuilder<?, ?> annotated,
                                                                      @NotNull AnnotationKind kind,
                                                                      int index, @NotNull InstanceType type) {
        A builder = AnnotationBuilder.newAnnotationBuilder(type);
        switch (kind) {
            case VIS_ANNO -> {
                if (index < 0) {
                    annotated.addVisibleRuntimeAnnotation(builder);
                } else {
                    annotated.setVisibleRuntimeAnnotation(index, builder);
                }
            }
            case INVIS_ANNO -> {
                if (index < 0) {
                    annotated.addInvisibleRuntimeAnnotation(builder);
                } else {
                    annotated.setInvisibleRuntimeAnnotation(index, builder);
                }
            }
            default -> throw new IllegalArgumentException("Invalid anno-kind: " + kind);
        }
        return builder;
    }
}
