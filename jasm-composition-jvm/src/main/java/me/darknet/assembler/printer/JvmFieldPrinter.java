package me.darknet.assembler.printer;

import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.constant.Constant;
import org.jetbrains.annotations.Nullable;

public class JvmFieldPrinter implements FieldPrinter {

    protected Field field;
    protected JvmMemberPrinter memberPrinter;

    public JvmFieldPrinter(Field field) {
        this.field = field;
        this.memberPrinter = new JvmMemberPrinter(field, JvmMemberPrinter.Type.FIELD);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        memberPrinter.printDeclaration(ctx)
                .literal(field.name())
                .print(" ")
                .literal(field.type().descriptor())
                .print(" ");
        Constant constant = field.defaultValue();
        if (constant != null) {
            ctx.print("{value: ");
            constant.accept(new ConstantPrinter(ctx));
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
