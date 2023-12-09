package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;

import dev.xdark.blw.annotation.ElementArrayBuilder;
import dev.xdark.blw.annotation.ElementEnum;
import dev.xdark.blw.annotation.ElementType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.Types;

public class BlwAnnotationArrayVisitor implements ASTAnnotationArrayVisitor, BlwElementAdapter {
    private final ElementArrayBuilder<?> builder;

    public BlwAnnotationArrayVisitor(ElementArrayBuilder<?> builder) {
        this.builder = builder;
    }

    @Override
    public void visitValue(ASTValue value) {
        builder.element(elementFromValue(value));
    }

    @Override
    public void visitTypeValue(ASTIdentifier className) {
        ObjectType type = Types.objectTypeFromInternalName(className.literal());
        builder.element(new ElementType(type));
    }

    @Override
    public void visitEnumValue(ASTIdentifier className, ASTIdentifier enumName) {
        InstanceType type = Types.instanceTypeFromInternalName(className.literal());
        builder.element(new ElementEnum(type, enumName.literal()));
    }

    @Override
    public ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier className) {
        InstanceType type = Types.instanceTypeFromInternalName(className.literal());
        return new BlwAnnotationVisitor(builder.annotation(type).child());
    }

    @Override
    public ASTAnnotationArrayVisitor visitArrayValue() {
        return new BlwAnnotationArrayVisitor(builder.array().child());
    }

    @Override
    public void visitEnd() {
    }
}
