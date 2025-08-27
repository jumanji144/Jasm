package me.darknet.assembler.printer;

import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.type.InstanceType;
import org.jetbrains.annotations.Nullable;

public class DalvikClassPrinter implements ClassPrinter {

    private final ClassDefinition definition;
    private final DalvikMemberPrinter memberPrinter;

    public DalvikClassPrinter(ClassDefinition definition) {
        this.definition = definition;
        this.memberPrinter = new DalvikMemberPrinter(definition, definition, DalvikMemberPrinter.Type.CLASS);
    }

    @Override
    public @Nullable MethodPrinter method(String name, String descriptor) {
        MethodMember method = definition.getMethod(name, descriptor);
        if (method != null) {
            return new DalvikMethodPrinter(method);
        }
        return null;
    }

    @Override
    public @Nullable FieldPrinter field(String name, String descriptor) {
        FieldMember field = definition.getField(name, descriptor);
        if (field != null) {
            return new DalvikFieldPrinter(field);
        }
        return null;
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
        String sourceFile = definition.getSourceFile();
        if (sourceFile != null) {
            ctx.begin().element(".sourcefile").string(sourceFile).end();
        }

        memberPrinter.printAttributes(ctx);

        var superClass = definition.getSuperClass();
        if (superClass != null) {
            ctx.begin().element(".super").literal(superClass.internalName()).end();
        }
        for (InstanceType anInterface : definition.getInterfaces()) {
            ctx.begin().element(".implements").literal(anInterface.internalName()).end();
        }
        var obj = memberPrinter.printDeclaration(ctx)
                .literal(definition.getType().internalName()).print(" ").declObject()
                .newline();
        for (var field : definition.getFields().values()) {
            var printer = new DalvikFieldPrinter(field);
            printer.print(obj);
            obj.next();
        }
        obj.line();
        for (var method : definition.getMethods().values()) {
            var printer = new DalvikMethodPrinter(method);
            printer.print(obj);
            obj.doubleNext();
        }
        obj.end();
    }
}
