package me.darknet.assembler.printer;

import me.darknet.assembler.DalvikModifiers;
import me.darknet.dex.tree.definitions.AccessFlags;
import me.darknet.dex.tree.definitions.Accessible;
import me.darknet.dex.tree.definitions.Annotated;
import me.darknet.dex.tree.definitions.Member;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.type.Type;

public record DalvikMemberPrinter(Annotated annotated, Accessible accessible, Type type) {

    public DalvikMemberPrinter(Member member, Type type) {
        this(member, member, type);
    }

    public void printAttributes(PrintContext<?> ctx) {
        if (annotated != null) {
            for (Annotation annotation : annotated.annotations()) {
                new DalvikAnnotationPrinter(annotation).print(ctx);
            }
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
                case CLASS -> DalvikModifiers.CLASS;
                case FIELD -> DalvikModifiers.FIELD;
                case METHOD -> DalvikModifiers.METHOD;
            };
            return ctx.begin().element(elementName)
                    .print(DalvikModifiers.modifiers(accessible.access(), modifierType));
        }
        return ctx;
    }

    public AnnotationPrinter printAnnotation(int index) {
        if (annotated != null) {
            return new DalvikAnnotationPrinter(annotated.annotations().get(index));
        }
        return null;
    }

    enum Type {
        CLASS,
        FIELD,
        METHOD
    }

}
