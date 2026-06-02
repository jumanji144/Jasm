package me.darknet.assembler.printer;

import me.darknet.assembler.util.JvmModifiers;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JvmClassPrinter implements ClassPrinter {
    protected final ClassNode view;
    protected final JvmMemberPrinter memberPrinter;

    public JvmClassPrinter(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    public JvmClassPrinter(InputStream stream) throws IOException {
        view = new ClassNode();
        new ClassReader(stream).accept(view, 0);
        this.memberPrinter = new JvmMemberPrinter(view, JvmMemberPrinter.Type.CLASS);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        for (InnerClassNode innerClass : view.innerClasses) {
            var obj = ctx.begin().element(".inner")
                    .print(JvmModifiers.modifiers(innerClass.access, JvmModifiers.CLASS)).object();
            if (innerClass.innerName != null) {
                obj.value("name").literal(innerClass.innerName).next();
            }
            obj.value("inner").literal(innerClass.name);
            if (innerClass.outerName != null) {
                obj.next();
                obj.value("outer").literal(innerClass.outerName);
            }
            obj.end();
            ctx.end();
        }

        if (view.sourceFile != null) {
            ctx.begin().element(".sourcefile").string(view.sourceFile).end();
        }
        if (view.sourceDebug != null) {
            ctx.begin().element(".source-debug-extension").string(view.sourceDebug).end();
        }
        if (view.outerClass != null) {
            ctx.begin().element(".outer-class").element(view.outerClass).end();
        }
        if (view.outerMethod != null && view.outerMethodDesc != null) {
            ctx.begin().element(".outer-method").element(view.outerMethod).element(view.outerMethodDesc).end();
        }
        if (view.nestHostClass != null) {
            ctx.begin().element(".nest-host").element(view.nestHostClass).end();
        }
        if (view.nestMembers != null && !view.nestMembers.isEmpty()) {
            for (String nestMember : view.nestMembers) {
                ctx.begin().element(".nest-member").element(nestMember).end();
            }
        }
        if (view.permittedSubclasses != null && !view.permittedSubclasses.isEmpty()) {
            for (String permittedSubclass : view.permittedSubclasses) {
                ctx.begin().element(".permitted-subclass").element(permittedSubclass).end();
            }
        }

        if (view.recordComponents != null && !view.recordComponents.isEmpty()) {
            for (RecordComponentNode recordComponent : view.recordComponents) {
                new JvmMemberPrinter(recordComponent).printAttributes(ctx);
                ctx.begin().element(".record-component").element(recordComponent.name).element(recordComponent.descriptor).end();
            }
        }

        memberPrinter.printAttributes(ctx);

        if (view.superName != null) {
            ctx.begin().element(".super").literal(view.superName).end();
        }
        if (view.interfaces != null) {
            for (String anInterface : view.interfaces) {
                ctx.begin().element(".implements").literal(anInterface).end();
            }
        }

        var obj = memberPrinter.printDeclaration(ctx).literal(view.name).print(" ").declObject().newline();
        for (FieldNode field : view.fields) {
            new JvmFieldPrinter(field).print(obj);
            obj.next();
        }
        obj.line();
        for (MethodNode method : view.methods) {
            new JvmMethodPrinter(method, Type.getObjectType(view.name)).print(obj);
            obj.doubleNext();
        }
        obj.end();
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

    @Override
    public MethodPrinter method(String name, String descriptor) {
        for (MethodNode method : view.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                return new JvmMethodPrinter(method, Type.getObjectType(view.name));
            }
        }
        return null;
    }

    @Override
    public FieldPrinter field(String name, String descriptor) {
        for (FieldNode field : view.fields) {
            if (field.name.equals(name) && field.desc.equals(descriptor)) {
                return new JvmFieldPrinter(field);
            }
        }
        return null;
    }
}

