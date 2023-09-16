package me.darknet.assembler.printer;

import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.constant.Constant;

public class JvmFieldPrinter implements FieldPrinter {

    protected Field field;
    protected MemberPrinter memberPrinter;

    public JvmFieldPrinter(Field field) {
        this.field = field;
        this.memberPrinter = new MemberPrinter(field, MemberPrinter.Type.FIELD);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        memberPrinter.printDeclaration(ctx).element(field.name()).element(field.type().descriptor());
        Constant constant = field.defaultValue();
        if (constant != null) {
            ctx.print("{value: ");
            constant.accept(new ConstantPrinter(ctx));
            ctx.print("}");
        }
    }

    @Override
    public AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }
}
