package me.darknet.assembler.printer;

import me.darknet.assembler.util.BlwModifiers;

import dev.xdark.blw.annotation.Annotation;
import dev.xdark.blw.classfile.Accessible;
import dev.xdark.blw.classfile.Annotated;
import dev.xdark.blw.classfile.Member;
import dev.xdark.blw.classfile.Signed;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record MemberPrinter(
        @Nullable Annotated annotated, @Nullable Signed signed, @Nullable Accessible accessible, Type type
) {

    public MemberPrinter(Member member, Type type) {
        this(member, member, member, type);
    }

    public void printAttributes(PrintContext<?> ctx) {
        if (annotated != null) {
            for (Annotation visibleRuntimeAnnotation : annotated.visibleRuntimeAnnotations()) {
                JvmAnnotationPrinter printer = JvmAnnotationPrinter.forTopLevelAnno(visibleRuntimeAnnotation, true);
                printer.print(ctx);
                ctx.next();
            }
            for (Annotation invisibleRuntimeAnnotation : annotated.invisibleRuntimeAnnotations()) {
                JvmAnnotationPrinter printer = JvmAnnotationPrinter.forTopLevelAnno(invisibleRuntimeAnnotation, false);
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
            return ctx.begin().element(elementName)
                    .print(BlwModifiers.modifiers(accessible.accessFlags(), modifierType));
        }
        return ctx;
    }

    /**
     * @param index Index into <i>all</i> annotations, where indices are based on the combined list of annotations.
     * @return Printer for the annotation. {@code null} if the index does not point to a known annotation.
     */
    public @Nullable AnnotationPrinter printAnnotation(int index) {
        if (annotated != null) {
            // First check visible annotations.
            List<Annotation> visibleAnnos = annotated.visibleRuntimeAnnotations();
            int runtimeAnnotationCount = visibleAnnos.size();
            if (index < runtimeAnnotationCount)
                return new JvmAnnotationPrinter(visibleAnnos.get(index), true);

            // Next check invisible annotations, offsetting the index by the number of visible annotations.
            List<Annotation> invisibleAnnos = annotated.invisibleRuntimeAnnotations();
            int targetInvisibleIndex = index - runtimeAnnotationCount;
            if (targetInvisibleIndex < invisibleAnnos.size())
                return new JvmAnnotationPrinter(invisibleAnnos.get(targetInvisibleIndex), false);
        }
        return null;
    }

    enum Type {
        CLASS,
        FIELD,
        METHOD
    }
}
