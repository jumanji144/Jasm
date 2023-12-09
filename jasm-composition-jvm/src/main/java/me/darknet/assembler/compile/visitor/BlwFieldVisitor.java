package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.util.ConstantMapper;
import me.darknet.assembler.visitor.ASTFieldVisitor;

import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.classfile.FieldBuilder;
import dev.xdark.blw.type.ClassType;

public class BlwFieldVisitor extends BlwMemberVisitor<ClassType, Field> implements ASTFieldVisitor {
    private final FieldBuilder<Field, ?> builder;

    public BlwFieldVisitor(FieldBuilder<Field, ?> builder) {
        super(builder);
        this.builder = builder;
    }

    @Override
    public void visitValue(ASTElement value) {
        if (value == null)
            return;
        builder.defaultValue(ConstantMapper.fromConstant(value));
    }

}
