package me.darknet.assembler.printer;

import me.darknet.assembler.DalvikModifiers;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.InnerClass;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.TypeConstant;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
        memberPrinter.printAttributes(ctx);
        if (definition.getSignature() != null) {
            ctx.begin().element(".signature").string(definition.getSignature()).next();
        }
        if (definition.getEnclosingClass() != null) {
            ctx.begin().element(".outer-class").literal(definition.getEnclosingClass().internalName()).next();
        }
        if (definition.getEnclosingMethod() != null) {
            ctx.begin().element(".outer-method")
                    .literal(definition.getEnclosingMethod().name()).print(" ")
                    .literal(definition.getEnclosingMethod().descriptor()).next();
        }
        printInnerClassMetadata(ctx);
        printMemberClassesMetadata(ctx);

        String sourceFile = definition.getSourceFile();
        if (sourceFile != null) {
            ctx.begin().element(".sourcefile").string(sourceFile).next();
        }

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

    private void printInnerClassMetadata(PrintContext<?> ctx) {
        String currentName = definition.getType().internalName();
        for (InnerClass innerClass : definition.getInnerClasses()) {
            if (!currentName.equals(innerClass.innerClassName())) {
                continue;
            }
            var object = ctx.begin().element(".inner")
                    .print(DalvikModifiers.modifiers(innerClass.access(), DalvikModifiers.CLASS))
                    .object();
            if (innerClass.innerName() != null) {
                object.literalValue("name").literal(innerClass.innerName()).next();
            }
            object.literalValue("inner").literal(innerClass.innerClassName()).next();
            object.literalValue("outer").literal(innerClass.outerClassName());
            object.end();
            ctx.next();
        }
    }

    private void printMemberClassesMetadata(PrintContext<?> ctx) {
        if (definition.getMemberClasses().isEmpty()) {
            return;
        }
        List<Constant> members = definition.getMemberClasses().stream()
                .map(TypeConstant::new)
                .map(constant -> (Constant) constant)
                .toList();
        AnnotationPart part = new AnnotationPart(
                Types.instanceTypeFromInternalName("dalvik/annotation/MemberClasses"),
                Map.of("value", new ArrayConstant(members))
        );
        new DalvikAnnotationPrinter(new Annotation((byte) Annotation.VISIBILITY_SYSTEM, part)).print(ctx);
    }
}
