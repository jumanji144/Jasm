package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.TypeReader;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.compile.BlwCompilerOptions;
import me.darknet.assembler.compile.builder.BlwReplaceClassBuilder;
import me.darknet.assembler.util.BlwModifiers;
import me.darknet.assembler.visitor.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlwClassVisitor implements ASTClassVisitor {

    private final BlwReplaceClassBuilder builder;
    private final BlwCompilerOptions options;
    private final List<InstanceType> interfaces = new ArrayList<>();

    public BlwClassVisitor(BlwCompilerOptions options, BlwReplaceClassBuilder builder) {
        this.options = options;
        this.builder = builder;
    }

    @Override
    public void visitSuperClass(ASTIdentifier superClass) {
        if (superClass == null) {
            return;
        }
        builder.superClass(Types.instanceTypeFromInternalName(superClass.literal()));
    }

    @Override
    public void visitInterface(ASTIdentifier interfaceName) {
        interfaces.add(Types.instanceTypeFromInternalName(interfaceName.literal()));
    }

    @Override
    public void visitSourceFile(ASTString sourceFile) {
        builder.sourceFile(sourceFile.content());
    }

    @Override
    public void visitInnerClass(Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
            ASTIdentifier innerClass) {
        // TODO: 01.09.23
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
        return new BlwMethodVisitor(builder.type, type, (accessFlags & AccessFlag.ACC_STATIC) == AccessFlag.ACC_STATIC,
                builder.method(accessFlags, name.literal(), type)
        );
    }

    @Override
    public ASTAnnotationVisitor visitAnnotation(ASTIdentifier classType) {
        // TODO: 01.09.23
        return null;
    }

    @Override
    public void visitSignature(ASTIdentifier signature) {
        // TODO: 01.09.23
    }

    @Override
    public void visitEnd() {
        builder.interfaces(interfaces);
    }
}
