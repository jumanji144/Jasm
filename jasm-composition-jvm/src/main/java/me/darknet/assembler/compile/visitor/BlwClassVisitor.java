package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.attribute.generic.GenericInnerClass;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.TypeReader;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.builder.BlwReplaceClassBuilder;
import me.darknet.assembler.util.BlwModifiers;
import me.darknet.assembler.util.CastUtil;
import me.darknet.assembler.visitor.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlwClassVisitor implements ASTClassVisitor {
    private final BlwReplaceClassBuilder builder;
    private final JvmCompilerOptions options;

    public BlwClassVisitor(JvmCompilerOptions options, BlwReplaceClassBuilder builder) {
        this.options = options;
        this.builder = builder;
    }

    @Override
    public void visitSuperClass(@Nullable ASTIdentifier superClass) {
        if (superClass == null)
            return;
        builder.setSuperClass(Types.instanceTypeFromInternalName(superClass.literal()));
    }

    @Override
    public void visitInterface(@NotNull ASTIdentifier interfaceName) {
        builder.addInterface(Types.instanceTypeFromInternalName(interfaceName.literal()));
    }

    @Override
    public void visitSourceFile(@Nullable ASTString sourceFile) {
        builder.setSourceFile(sourceFile == null ? null : sourceFile.content());
    }

    @Override
    public void visitInnerClass(Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
            ASTIdentifier innerClass) {
        int accessFlags = BlwModifiers.getClassModifiers(modifiers);
        InstanceType type = innerClass == null ? null : Types.instanceTypeFromInternalName(innerClass.literal());
        InstanceType outerType = outerClass == null ? null : Types.instanceTypeFromInternalName(outerClass.literal());
        String innerName = name == null ? null : name.literal();
        builder.addInnerClass(new GenericInnerClass(accessFlags, type, outerType, innerName));
    }

    @Override
    public ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        int accessFlags = BlwModifiers.getFieldModifiers(modifiers);
        return new BlwFieldVisitor(
                builder.putField(accessFlags, name.literal(), new TypeReader(descriptor.literal()).requireClassType())
                        .child()
        );
    }

    @Override
    public ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        int accessFlags = BlwModifiers.getMethodModifiers(modifiers);
        MethodType type = Types.methodType(descriptor.literal());
        return new BlwMethodVisitor(
                options, builder.type(), type, (accessFlags & AccessFlag.ACC_STATIC) == AccessFlag.ACC_STATIC,
                CastUtil.cast(builder.putMethod(accessFlags, name.literal(), type).child()),
                analysisResults -> builder.setMethodAnalysis(name.literal(), type, analysisResults)
        );
    }

    @Override
    public ASTAnnotationVisitor visitAnnotation(ASTIdentifier classType) {
        InstanceType type = Types.instanceTypeFromInternalName(classType.literal());
        return new BlwAnnotationVisitor(builder.putVisibleRuntimeAnnotation(type).child());
    }

    @Override
    public void visitSignature(@Nullable ASTString signature) {
        if (signature != null)
            builder.signature(signature.content());
    }

    @Override
    public void visitEnd() {
    }
}
