package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.TypeReader;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.BlwCompilerOptions;
import me.darknet.assembler.compile.builder.BlwReplaceClassBuilder;
import me.darknet.assembler.util.BlwModifiers;
import me.darknet.assembler.visitor.*;

public record BlwRootVisitor(BlwReplaceClassBuilder builder, BlwCompilerOptions options) implements ASTRootVisitor {

    @Override
    public ASTAnnotationVisitor visitAnnotation(ASTIdentifier name) {
        // parse annotation path
        var path = options.annotationPath().split("\\.");
        String className = path[0];
        int index = Integer.parseInt(path[path.length - 1]);

        InstanceType type = Types.instanceTypeFromInternalName(className);

        return new BlwAnnotationVisitor(switch (path.length) {
            case 2 -> builder.visibleRuntimeAnnotation(type, index);
            case 5 -> {
                String member = path[3];
                String descriptor = path[4];
                yield switch (path[2]) {
                    case "field" -> builder.fields.get(member + descriptor).visibleRuntimeAnnotation(type, index);
                    case "method" -> builder.methods.get(member + descriptor).visibleRuntimeAnnotation(type, index);
                    default -> throw new IllegalStateException("Unexpected value: " + path[2]);
                };
            }
            default -> throw new IllegalStateException("Unexpected value: " + path.length);
        });
    }

    @Override
    public ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name) {
        int accessFlags = modifiers.modifiers().stream()
                .map(it -> BlwModifiers.modifier(it.content(), BlwModifiers.CLASS))
                .reduce(0, (a, b) -> a | b);
        builder.accessFlags(accessFlags);
        builder.type(Types.instanceTypeFromInternalName(name.literal()));
        return new BlwClassVisitor(options, builder);
    }

    @Override
    public ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        int accessFlags = modifiers.modifiers().stream()
                .map(it -> BlwModifiers.modifier(it.content(), BlwModifiers.FIELD))
                .reduce(0, (a, b) -> a | b);
        return new BlwFieldVisitor(
                builder.field(accessFlags, name.literal(), new TypeReader(descriptor.literal()).requireClassType())
        );
    }

    @Override
    public ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        int accessFlags = modifiers.modifiers().stream()
                .map(it -> BlwModifiers.modifier(it.content(), BlwModifiers.METHOD))
                .reduce(0, (a, b) -> a | b);
        MethodType type = Types.methodType(descriptor.literal());
        return new BlwMethodVisitor(Types.instanceType(Object.class), type,
                (accessFlags & AccessFlag.ACC_STATIC) == AccessFlag.ACC_STATIC,
            builder.method(accessFlags, name.literal(), type)
        );
    }
}
