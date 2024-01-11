package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.*;

public class JvmAnnotationPrinter implements AnnotationPrinter {

    private final Annotation annotation;

    public JvmAnnotationPrinter(Annotation annotation) {
        this.annotation = annotation;
    }

    void printElement(PrintContext<?> ctx, Element element) {
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
            if (!content.matches("[\\D]+"))
                ctx.print( "D");
        } else if (element instanceof ElementString es) {
            ctx.string(es.value());
        } else if (element instanceof ElementBoolean eb) {
            ctx.print(Boolean.toString(eb.value()));
        } else if (element instanceof ElementByte eb) {
            ctx.print(Byte.toString(eb.value()));
            // TODO char parsing | case ElementChar ec -> ctx.print("'" + ec.value() + "'");
        } else if (element instanceof ElementShort es) {
            ctx.print(Short.toString(es.value()));
        } else if (element instanceof ElementEnum ee) {
            ctx.element(".enum").literal(ee.type().internalName()).print(".").literal(ee.name());
        } else if (element instanceof ElementType et) {
            ctx.print("L").literal(et.value().internalName()).literal(";");
        } else if (element instanceof Annotation) {
            Annotation ea = (Annotation) element;
            JvmAnnotationPrinter printer = new JvmAnnotationPrinter(ea);
            printer.print(ctx);
        } else if (element instanceof ElementArray ea) {
            var array = ctx.array();
            for (var entry : ea) {
                printElement(array, entry);
                array.arg();
            }
            array.end();
        } else {
            throw new IllegalStateException("Unexpected value: " + element);
        }
    }

    @Override
    public void print(PrintContext<?> ctx) {
        ctx.begin().element(".annotation").literal(annotation.type().internalName()).print(" ");
        if (annotation.names().isEmpty()) {
            ctx.print("{}");
            return;
        }
        var obj = ctx.object();
        for (var entry : annotation) {
            obj.literalValue(entry.getKey());
            printElement(obj, entry.getValue());
            obj.next();
        }
        obj.end();
    }
}
