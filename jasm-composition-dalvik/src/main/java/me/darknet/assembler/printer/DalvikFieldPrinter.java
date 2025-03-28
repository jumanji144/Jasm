package me.darknet.assembler.printer;

import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.constant.Constant;
import org.jetbrains.annotations.Nullable;

public class DalvikFieldPrinter implements FieldPrinter {

    private final FieldMember definition;
    private final DalvikMemberPrinter memberPrinter;

    public DalvikFieldPrinter(FieldMember definition) {
        this.definition = definition;
        this.memberPrinter = new DalvikMemberPrinter(definition, definition, DalvikMemberPrinter.Type.FIELD);
    }

    @Override
    public @Nullable AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter visibleAnnotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter invisibleAnnotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        memberPrinter.printDeclaration(ctx).literal(definition.name()).print(" ").literal(definition.type().descriptor())
                .print(" ");
        Constant constant = definition.staticValue();
        if (constant != null) {
            ctx.print("{value: ");
            ConstantPrinter.printConstant(ctx, constant);
            ctx.print("}");
        }
    }
}
