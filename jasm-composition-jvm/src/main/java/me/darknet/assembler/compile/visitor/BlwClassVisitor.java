package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.attribute.generic.GenericInnerClass;
import dev.xdark.blw.type.*;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.builder.BlwReplaceClassBuilder;
import me.darknet.assembler.util.BlwModifiers;
import me.darknet.assembler.util.CastUtil;
import me.darknet.assembler.visitor.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    public void visitOuterClass(@Nullable ASTElement outerClass) {
        builder.setOuterClass(outerClass == null ? null : outerClass.content());
    }

    @Override
    public void visitOuterMethod(@Nullable ASTOuterMethod outerMethod) {
        if (outerMethod == null) return;
        String outerClass = builder.getOuterClass();
        if (outerClass == null) throw new IllegalStateException("Must visit outer class attribute before visiting outer method");
        builder.setOuterMethod(outerClass, outerMethod.getMethodName().content(), outerMethod.getMethodDesc().content());
    }

    @Override
    public void visitPermittedSubclass(@NotNull ASTIdentifier subclass) {
        builder.addPermittedSubclass(Types.instanceTypeFromInternalName(subclass.literal()));
    }

    @Override
    public void visitNestHost(@Nullable ASTIdentifier nestHost) {
        builder.setNestHost(nestHost == null ? null : Types.instanceTypeFromInternalName(nestHost.literal()));
    }

    @Override
    public void visitNestMember(@NotNull ASTIdentifier nestMember) {
        builder.addNestMember(Types.instanceTypeFromInternalName(nestMember.literal()));
    }

    @Override
    public ASTRecordComponentVisitor visitRecordComponent(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor, @Nullable ASTString signature) {
	    String componentDesc = descriptor.literal();
	    Type type = Types.typeFromDescriptor(componentDesc);
        if (type instanceof ClassType descClassType) {
            var component = builder.putRecordComponent(name.content(), descClassType, signature == null ? null : signature.content()).child();
            return new BlwRecordComponentVisitor(component);
        }
		throw new IllegalStateException("Illegal record component type: " + componentDesc);
    }

    @Override
    public void visitInnerClass(@NotNull Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
                                @Nullable ASTIdentifier innerClass) {
        int accessFlags = BlwModifiers.getClassModifiers(modifiers);
        InstanceType type = innerClass == null ? null : Types.instanceTypeFromInternalName(innerClass.literal());
        InstanceType outerType = outerClass == null ? null : Types.instanceTypeFromInternalName(outerClass.literal());
        String innerName = name == null ? null : name.literal();
        builder.addInnerClass(new GenericInnerClass(accessFlags, type, outerType, innerName));
    }

    @Override
    public ASTFieldVisitor visitField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor) {
        int accessFlags = BlwModifiers.getFieldModifiers(modifiers);
        return new BlwFieldVisitor(
                builder.putField(accessFlags, name.literal(), new TypeReader(descriptor.literal()).requireClassType())
                        .child()
        );
    }

    @Override
    public ASTMethodVisitor visitMethod(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor) {
        int accessFlags = BlwModifiers.getMethodModifiers(modifiers);
        MethodType type = Types.methodType(descriptor.literal());
        return new BlwMethodVisitor(
                options, builder.type(), type, (accessFlags & AccessFlag.ACC_STATIC) == AccessFlag.ACC_STATIC,
                CastUtil.cast(builder.putMethod(accessFlags, name.literal(), type).child()),
                analysisResults -> builder.setMethodAnalysis(name.literal(), type, analysisResults)
        );
    }

    @Override
    public ASTAnnotationVisitor visitVisibleAnnotation(@NotNull ASTIdentifier classType) {
        InstanceType type = Types.instanceTypeFromInternalName(classType.literal());
        return new BlwAnnotationVisitor(builder.addVisibleRuntimeAnnotation(type).child());
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType) {
        InstanceType type = Types.instanceTypeFromInternalName(classType.literal());
        return new BlwAnnotationVisitor(builder.addInvisibleRuntimeAnnotation(type).child());
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
