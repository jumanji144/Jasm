package me.darknet.assembler.printer;

import me.darknet.assembler.util.EscapeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.lang.reflect.Array;
import java.util.List;

public class JvmAnnotationPrinter implements AnnotationPrinter {
    protected final AnnotationNode annotation;
    protected final Boolean visible;

    protected JvmAnnotationPrinter(@Nullable AnnotationNode annotation, boolean visible) {
        this.annotation = annotation;
        this.visible = visible;
    }

    protected JvmAnnotationPrinter(@Nullable AnnotationNode annotation) {
        this.annotation = annotation;
        this.visible = null;
    }

    public static JvmAnnotationPrinter forTopLevelAnno(@NotNull AnnotationNode annotation, boolean visible) {
        if (annotation instanceof TypeAnnotationNode typeAnnotation) {
            return new JvmTypeAnnotationPrinter(typeAnnotation, visible);
        }
        return new JvmAnnotationPrinter(annotation, visible);
    }

    public static JvmAnnotationPrinter forEmbeddedAnno(@NotNull AnnotationNode annotation) {
        if (annotation instanceof TypeAnnotationNode typeAnnotation) {
            return new JvmTypeAnnotationPrinter(typeAnnotation);
        }
        return new JvmAnnotationPrinter(annotation);
    }

    public static JvmAnnotationPrinter forElements() {
        return new JvmAnnotationPrinter(null);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        String token = visible == null ? ".annotation" : visible ? ".visible-annotation" : ".invisible-annotation";
        ctx.begin().element(token).literal(Type.getType(annotation.desc).getInternalName()).print(" ");
        if (annotation.values == null || annotation.values.isEmpty()) {
            ctx.print("{}");
            return;
        }
        var obj = ctx.object();
        printEntries(obj, annotation.values);
        obj.end();
    }

    public void printAnnotation(@NotNull PrintContext<?> ctx, @NotNull AnnotationNode annotation) {
        forEmbeddedAnno(annotation).print(ctx);
    }

    protected void printEntries(@NotNull PrintContext.ObjectPrint ctx, @NotNull List<Object> values) {
        for (int i = 0; i < values.size(); i += 2) {
            if (i > 0) {
                ctx.next();
            }
            ctx.literalValue((String) values.get(i));
            printElement(ctx, values.get(i + 1));
        }
    }

    public void printElement(@NotNull PrintContext<?> ctx, @Nullable Object element) {
        switch (element) {
            case null -> ctx.print("null");
            case Integer ei -> ctx.print(Integer.toString(ei));
            case Long el -> ctx.print(el + "L");
            case Float ef -> ctx.print(ef + "F");
            case Double ed -> {
                String content = Double.toString(ed);
                ctx.print(content);
                if (!content.matches("\\D+")) {
                    ctx.print("D");
                }
            }
            case String es -> ctx.string(es);
            case Boolean eb -> ctx.print(Boolean.toString(eb));
            case Byte eb -> ctx.print(Byte.toString(eb));
            case Character ec -> {
                String str = String.valueOf(ec);
                ctx.print("'").print(EscapeUtil.escapeString(str)).print("'");
            }
            case Short es -> ctx.print(Short.toString(es));
            case String[] enumValue -> ctx.element(".enum").literal(Type.getType(enumValue[0]).getInternalName()).print(" ").literal(enumValue[1]);
            case Type et -> JvmConstantPrinter.printTypeLiteral(et, ctx);
            case AnnotationNode ea -> printAnnotation(ctx, ea);
            case List<?> ea -> {
                var array = ctx.array();
                boolean first = true;
                for (Object value : ea) {
                    if (!first) {
                        array.arg();
                    }
                    printElement(array, value);
                    first = false;
                }
                array.end();
            }
            default -> {
                if (element.getClass().isArray()) {
                    var array = ctx.array();
                    int length = Array.getLength(element);
                    for (int i = 0; i < length; i++) {
                        if (i > 0) {
                            array.arg();
                        }
                        printElement(array, Array.get(element, i));
                    }
                    array.end();
                } else {
                    throw new IllegalStateException("Unexpected annotation value: " + element);
                }
            }
        }
    }
}
