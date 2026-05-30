package me.darknet.assembler.printer;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;

public class JvmFieldPrinter implements FieldPrinter {
    protected final FieldNode field;
    protected final JvmMemberPrinter memberPrinter;

    public JvmFieldPrinter(FieldNode field) {
        this.field = field;
        this.memberPrinter = new JvmMemberPrinter(field, JvmMemberPrinter.Type.FIELD);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        memberPrinter.printDeclaration(ctx)
                .literal(field.name)
                .print(" ")
                .literal(field.desc)
                .print(" ");
        if (field.value != null) {
            ctx.print("{value: ");
            new JvmConstantPrinter(ctx).printConstant(field.value);
            ctx.print("}");
        }
    }

    @Override
    public @Nullable AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter visibleAnnotation(int index) {
        return memberPrinter.printVisibleAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter invisibleAnnotation(int index) {
        return memberPrinter.printInvisibleAnnotation(index);
    }
}
