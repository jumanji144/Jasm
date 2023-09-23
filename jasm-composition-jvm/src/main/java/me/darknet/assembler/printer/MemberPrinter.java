package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.Annotation;
import dev.xdark.blw.classfile.Accessible;
import dev.xdark.blw.classfile.Annotated;
import dev.xdark.blw.classfile.Member;
import dev.xdark.blw.classfile.Signed;
import me.darknet.assembler.util.BlwModifiers;
import org.jetbrains.annotations.Nullable;

public record MemberPrinter(
        @Nullable Annotated annotated, @Nullable Signed signed, @Nullable Accessible accessible, Type type
) {

    public MemberPrinter(Member member, Type type) {
        this(member, member, member, type);
    }

    public void printAttributes(PrintContext<?> ctx) {
        if (annotated != null) {
            for (Annotation invisibleRuntimeAnnotation : annotated.invisibleRuntimeAnnotations()) {
                JvmAnnotationPrinter printer = new JvmAnnotationPrinter(invisibleRuntimeAnnotation);
                printer.print(ctx);
                ctx.next();
            }
            for (Annotation invisibleTypeAnnotation : annotated.visibleRuntimeAnnotations()) {
                JvmAnnotationPrinter printer = new JvmAnnotationPrinter(invisibleTypeAnnotation);
                printer.print(ctx);
                ctx.next();
            }
        }
        if (signed != null && signed.signature() != null) {
            ctx.begin().element(".signature").string(signed.signature()).next();
        }
    }

    public PrintContext<?> printDeclaration(PrintContext<?> ctx) {
        if (accessible != null) {
            String elementName = switch (type) {
                case CLASS -> ".class";
                case FIELD -> ".field";
                case METHOD -> ".method";
            };
            int modifierType = switch (type) {
                case CLASS -> BlwModifiers.CLASS;
                case FIELD -> BlwModifiers.FIELD;
                case METHOD -> BlwModifiers.METHOD;
            };
            return ctx.begin().element(elementName).print(BlwModifiers.modifiers(accessible.accessFlags(), modifierType));
        }
        return ctx;
    }

    public AnnotationPrinter printAnnotation(int index) {
        if (annotated != null) {
            // go through the annotations
            for (int i = 0; i < annotated.visibleRuntimeAnnotations().size(); i++) {
                if (i == index) {
                    return new JvmAnnotationPrinter(annotated.visibleRuntimeAnnotations().get(i));
                }
            }
            for (int i = annotated.visibleRuntimeAnnotations().size(); i < annotated.invisibleRuntimeAnnotations()
                    .size() + annotated.visibleRuntimeAnnotations().size(); i++) {
                if (i == index) {
                    return new JvmAnnotationPrinter(annotated.invisibleRuntimeAnnotations().get(i));
                }
            }
        }
        return null;
    }

    enum Type {
        CLASS,
        FIELD,
        METHOD
    }
}
