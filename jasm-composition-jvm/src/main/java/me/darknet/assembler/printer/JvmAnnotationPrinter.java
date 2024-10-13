package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.*;
import me.darknet.assembler.util.EscapeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class JvmAnnotationPrinter implements AnnotationPrinter {

    private final Annotation annotation;
    private final Boolean visible;

    protected JvmAnnotationPrinter(Annotation annotation, boolean visible) {
        this.annotation = annotation;
        this.visible = visible;
    }

    protected JvmAnnotationPrinter(Annotation annotation) {
        this.annotation = annotation;
        this.visible = null;
    }

    public static JvmAnnotationPrinter forTopLevelAnno(Annotation annotation, boolean visible) {
        return new JvmAnnotationPrinter(annotation, visible);
    }

    public static JvmAnnotationPrinter forEmbeddedAnno(Annotation annotation) {
        return new JvmAnnotationPrinter(annotation);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        // For embedded annotations (an annotation inside another) we do not have any concept
        // of 'visible' vs 'invisible' annotations, so we'll shorten the name.
        String token = visible == null ? ".annotation" :
                visible ? ".visible-annotation" : ".invisible-annotation";

        Annotation annotation = this.annotation;
        ctx.begin().element(token).literal(annotation.type().internalName()).print(" ");
        if (annotation.names().isEmpty()) {
            ctx.print("{}");
            return;
        }
        var obj = ctx.object();
        obj.print(annotation, this::printEntry);
        obj.end();
    }

    public void printAnnotation(@NotNull PrintContext<?> ctx, @NotNull Annotation annotation) {
        forEmbeddedAnno(annotation).print(ctx);
    }

    private void printEntry(@NotNull PrintContext.ObjectPrint ctx, @NotNull Map.Entry<String, Element> entry) {
        ctx.literalValue(entry.getKey());
        printElement(ctx, entry.getValue());
    }

    public void printElement(@NotNull PrintContext<?> ctx, @NotNull Element element) {
        if (element instanceof ElementInt ei) {
            ctx.print(Integer.toString(ei.value()));
        } else if (element instanceof ElementLong el) {
            ctx.print(el.value() + "L");
        } else if (element instanceof ElementFloat ef) {
            ctx.print(ef.value() + "F");
        } else if (element instanceof ElementDouble ed) {
            String content = Double.toString(ed.value());
            ctx.print(content);

            // Skip 'D' suffix for things like 'NaN' where it is implied
            if (!content.matches("\\D+"))
                ctx.print( "D");
        } else if (element instanceof ElementString es) {
            ctx.string(es.value());
        } else if (element instanceof ElementBoolean eb) {
            ctx.print(Boolean.toString(eb.value()));
        } else if (element instanceof ElementByte eb) {
            ctx.print(Byte.toString(eb.value()));
        } else if (element instanceof ElementChar ec) {
            String str = String.valueOf(ec.value());
            ctx.print("'").print(EscapeUtil.escapeString(str)).print("'");
        } else if (element instanceof ElementShort es) {
            ctx.print(Short.toString(es.value()));
        } else if (element instanceof ElementEnum ee) {
            ctx.element(".enum").literal(ee.type().internalName()).print(" ").literal(ee.name());
        } else if (element instanceof ElementType et) {
            ctx.literal(et.value().internalName());
        } else if (element instanceof Annotation ea) {
            printAnnotation(ctx, ea);
        } else if (element instanceof ElementArray ea) {
            var array = ctx.array();
            array.print(ea, this::printElement);
            array.end();
        } else {
            throw new IllegalStateException("Unexpected value: " + element);
        }
    }
}
