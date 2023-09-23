package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.MemberBuilder;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import org.jetbrains.annotations.Nullable;

public class BlwMemberVisitor implements ASTDeclarationVisitor {

    private final MemberBuilder.Nested<?> builder;

    public BlwMemberVisitor(MemberBuilder.Nested<?> builder) {
        this.builder = builder;
    }

    @Override
    public ASTAnnotationVisitor visitAnnotation(ASTIdentifier classType) {
        return new BlwAnnotationVisitor(
                builder.visibleRuntimeAnnotation(Types.instanceTypeFromInternalName(classType.literal()))
        );
    }

    @Override
    public void visitSignature(@Nullable ASTString signature) {
        builder.signature(signature.content());
    }

    @Override
    public void visitEnd() {
    }
}
