package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.*;

public class BlwAnnotationPrinter implements AnnotationPrinter {

    private final Annotation annotation;

    public BlwAnnotationPrinter(Annotation annotation) {
        this.annotation = annotation;
    }

    void printElement(PrintContext<?> ctx, Element element) {
		switch (element) {
			case ElementInt ei -> ctx.print(Integer.toString(ei.value()));
			case ElementLong el -> ctx.print(el.value() + "L");
			case ElementFloat ef -> ctx.print(ef.value() + "F");
			case ElementDouble ed -> ctx.print(ed.value() + "D");
			case ElementString es -> ctx.string(es.value());
			case ElementBoolean eb -> ctx.print(Boolean.toString(eb.value()));
			case ElementByte eb -> ctx.print(Byte.toString(eb.value()));
			// TODO char parsing | case ElementChar ec -> ctx.print("'" + ec.value() + "'");
			case ElementShort es -> ctx.print(Short.toString(es.value()));
			case ElementEnum ee -> ctx.element(".enum")
					.literal(ee.type().internalName())
					.print(".")
					.literal(ee.name());
			case ElementType et -> ctx.print("L").literal(et.value().internalName()).literal(";");
			case Annotation ea -> {
				BlwAnnotationPrinter printer = new BlwAnnotationPrinter(ea);
				printer.print(ctx);
			}
			case ElementArray ea -> {
				var array = ctx.array();
				for (var entry : ea) {
					printElement(array, entry);
					array.arg();
				}
				array.end();
			}
			default -> throw new IllegalStateException("Unexpected value: " + element);
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
