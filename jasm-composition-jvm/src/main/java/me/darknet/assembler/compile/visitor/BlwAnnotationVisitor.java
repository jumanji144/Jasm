package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;

import dev.xdark.blw.annotation.AnnotationBuilder;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.Types;

public class BlwAnnotationVisitor implements ASTAnnotationVisitor, BlwElementAdapter {
    private final AnnotationBuilder<?> builder;

    public BlwAnnotationVisitor(AnnotationBuilder<?> builder) {
        this.builder = builder;
    }

    @Override
    public void visitValue(ASTIdentifier name, ASTValue value) {
        String nameLiteral = name.literal();
        builder.element(nameLiteral, elementFromValue(value));
    }

    @Override
    public void visitTypeValue(ASTIdentifier name, ASTIdentifier className) {
        String nameLiteral = name.literal();
        builder.element(nameLiteral, elementFromTypeIdentifier(className));
    }

    @Override
    public void visitEnumValue(ASTIdentifier name, ASTIdentifier className, ASTIdentifier enumName) {
        builder.element(name.literal(), elementFromEnum(className, enumName));
    }

    @Override
    public ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier name, ASTIdentifier className) {
        InstanceType type = Types.instanceTypeFromInternalName(className.literal());
        return new BlwAnnotationVisitor(builder.annotation(name.literal(), type).child());
    }

    @Override
    public ASTAnnotationArrayVisitor visitArrayValue(ASTIdentifier name) {
        return new BlwAnnotationArrayVisitor(builder.array(name.literal()).child());
    }

    @Override
    public void visitEnd() {
    }
}
