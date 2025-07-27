package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.annotation.TypePath;
import dev.xdark.blw.type.InstanceType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;

import dev.xdark.blw.classfile.Member;
import dev.xdark.blw.classfile.MemberBuilder;
import dev.xdark.blw.type.Type;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlwMemberVisitor<T extends Type, M extends Member<T>> implements ASTDeclarationVisitor {
    private final MemberBuilder<T, M, ?> builder;

    public BlwMemberVisitor(MemberBuilder<T, M, ?> builder) {
        this.builder = builder;
    }

    @Override
    public ASTAnnotationVisitor visitVisibleAnnotation(@NotNull ASTIdentifier classType) {
        return new BlwAnnotationVisitor(
                builder.addVisibleRuntimeAnnotation(Types.instanceTypeFromInternalName(classType.literal())).child()
        );
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType) {
        return new BlwAnnotationVisitor(
                builder.addInvisibleRuntimeAnnotation(Types.instanceTypeFromInternalName(classType.literal())).child()
        );
    }

    @Override
    public ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        InstanceType type = Types.instanceTypeFromInternalName(classType.literal());
        return new BlwAnnotationVisitor(builder.addVisibleRuntimeTypeAnnotation(type, typeRef.asInt(),
                typePath == null ? null : TypePath.fromString(typePath.content())).child());
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        InstanceType type = Types.instanceTypeFromInternalName(classType.literal());
        return new BlwAnnotationVisitor(builder.addInvisibleRuntimeTypeAnnotation(type, typeRef.asInt(),
                typePath == null ? null : TypePath.fromString(typePath.content())).child());

    }

    @Override
    public void visitSignature(@Nullable ASTString signature) {
        if (signature != null) builder.signature(signature.content());
    }

    @Override
    public void visitEnd() {
    }
}
