package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.MemberBuilder;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;

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
    public void visitSignature(ASTIdentifier signature) {
        builder.signature(signature.literal());
    }

    @Override
    public void visitEnd() {}
}
