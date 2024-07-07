package me.darknet.assembler.printer;

import dev.xdark.blw.classfile.RecordComponent;
import me.darknet.assembler.util.BlwModifiers;

import dev.xdark.blw.BytecodeLibrary;
import dev.xdark.blw.asm.AsmBytecodeLibrary;
import dev.xdark.blw.asm.ClassWriterProvider;
import dev.xdark.blw.classfile.ClassFileView;
import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.attribute.InnerClass;
import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import dev.xdark.blw.type.InstanceType;
import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class JvmClassPrinter implements ClassPrinter {

    protected ClassFileView view;
    protected MemberPrinter memberPrinter;
    private static final BytecodeLibrary library = new AsmBytecodeLibrary(
            ClassWriterProvider.flags(ClassWriter.COMPUTE_FRAMES)
    );

    public JvmClassPrinter(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    public JvmClassPrinter(InputStream stream) throws IOException {
        var builder = new GenericClassBuilder();
        library.read(stream, builder);
        view = builder.build();
        this.memberPrinter = new MemberPrinter(view, view, view, MemberPrinter.Type.CLASS);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        for (InnerClass innerClass : view.innerClasses()) {
            var obj = ctx.begin().element(".inner")
                    .print(BlwModifiers.modifiers(innerClass.accessFlags(), BlwModifiers.CLASS)).object();
            String name = innerClass.innerName();
            if (name != null) {
                obj.value("name").literal(name).next();
            }
            obj.value("inner").literal(innerClass.type().internalName());
            InstanceType outer = innerClass.outerType();
            if (outer != null) {
                obj.next();
                obj.value("outer").literal(outer.internalName());
            }
            obj.end();
            ctx.end();
        }

        String sourceFile = view.sourceFile();
        if (sourceFile != null) {
            ctx.begin().element(".sourcefile").string(sourceFile).end();
        }

        String outerClass = view.outerClass();
        if (outerClass != null) {
            ctx.begin().element(".outer-class").element(outerClass).end();
        }

        String outerMethodName = view.outerMethodName();
        String outerMethodDesc = view.outerMethodDescriptor();
        if (outerMethodName != null && outerMethodDesc != null) {
            ctx.begin().element(".outer-method")
                    .element(outerMethodName).element(outerMethodDesc).end();
        }

        InstanceType nestHost = view.nestHost();
        if (nestHost != null)
            ctx.begin().element(".nest-host").element(nestHost.internalName()).end();
        List<InstanceType> nestMembers = view.nestMembers();
        if (nestMembers != null && !nestMembers.isEmpty()) {
            for (InstanceType nestMember : nestMembers)
                ctx.begin().element(".nest-member").element(nestMember.internalName()).end();
        }

        List<InstanceType> permittedSubclasses = view.permittedSubclasses();
        if (permittedSubclasses != null && !permittedSubclasses.isEmpty()) {
            permittedSubclasses.forEach(t -> ctx.begin().element(".permitted-subclass").element(t.internalName()).end());
        }

        // Record components in JASM consume attributes like Signatures and Annotations.
        // So we cannot put either of those things before we print these.
        List<RecordComponent> recordComponents = view.recordComponents();
        if (recordComponents != null && !recordComponents.isEmpty()) {
            recordComponents.forEach(r -> {
                // Dirty hack to print the records signature/annotations without too much copy-pasting
                var compAttrPrinter = new MemberPrinter(r, r, view, MemberPrinter.Type.CLASS);
                compAttrPrinter.printAttributes(ctx);
                ctx.begin().element(".record-component").element(r.name()).element(r.type().descriptor()).end();
            });
        }

        // This prints attributes that are held mutually by classes and members, so:
        //  - Signature
        //  - Annotations
        // NOTE: This must be called AFTER record-components are printed
        memberPrinter.printAttributes(ctx);

        var superClass = view.superClass();
        if (superClass != null)
            ctx.begin().element(".super").literal(superClass.internalName()).end();
        for (InstanceType anInterface : view.interfaces()) {
            ctx.begin().element(".implements").literal(anInterface.internalName()).end();
        }
        var obj = memberPrinter.printDeclaration(ctx)
                .literal(view.type().internalName()).print(" ").declObject()
                .newline();
        for (Field field : view.fields()) {
            JvmFieldPrinter printer = new JvmFieldPrinter(field);
            printer.print(obj);
            obj.next();
        }
        obj.line();
        for (Method method : view.methods()) {
            JvmMethodPrinter printer = new JvmMethodPrinter(method);
            printer.print(obj);
            obj.doubleNext();
        }
        obj.end();
    }

    @Override
    public AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public MethodPrinter method(String name, String descriptor) {
        // find method
        for (Method method : view.methods()) {
            if (method.name().equals(name) && method.type().descriptor().equals(descriptor)) {
                return new JvmMethodPrinter(method);
            }
        }
        return null;
    }

    @Override
    public FieldPrinter field(String name, String descriptor) {
        // find field
        for (Field field : view.fields()) {
            if (field.name().equals(name) && field.type().descriptor().equals(descriptor)) {
                return new JvmFieldPrinter(field);
            }
        }
        return null;
    }
}
