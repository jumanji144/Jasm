package me.darknet.assembler.printer;

import me.darknet.assembler.util.EscapeUtil;
import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.constant.*;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public record DalvikAnnotationPrinter(Annotation annotation) implements AnnotationPrinter {

    public final static byte VISIBILITY_INTERNAL = Annotation.VISIBILITY_SYSTEM + 1;

    @Override
    public void print(PrintContext<?> ctx) {
        var part = annotation.annotation();
        var visibility = annotation.visibility();

        String token = switch (visibility) {
            case Annotation.VISIBILITY_BUILD -> ".invisible-annotation";
            case Annotation.VISIBILITY_RUNTIME -> ".visible-annotation";
            case Annotation.VISIBILITY_SYSTEM -> ".system-annotation";
            case VISIBILITY_INTERNAL -> ".annotation";
            default -> throw new IllegalStateException("Unexpected value: " + visibility);
        };

        ctx.begin().element(token).literal(part.type().internalName()).print(" ");
        if (part.elements().isEmpty()) {
            ctx.print("{}");
            return;
        }

        var obj = ctx.object();
        obj.print(new Iterable<>() {
            @Override
            public @NotNull Iterator<Map.Entry<String, Constant>> iterator() {
                return part.elements().entrySet().iterator();
            }
        }, this::printEntry);
        obj.end();

        ctx.append(".end annotation\n");
    }

    private void printAnnotation(PrintContext<?> ctx, AnnotationPart part) {
        var printer = new DalvikAnnotationPrinter(new Annotation(VISIBILITY_INTERNAL, part));
        printer.print(ctx);
    }

    private void printEntry(PrintContext.ObjectPrint ctx, Map.Entry<String, Constant> entry) {
        ctx.literalValue(entry.getKey());
        ConstantPrinter.printConstant(ctx, entry.getValue());
    }
}
