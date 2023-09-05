package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.FieldBuilder;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.util.ConstantMapper;
import me.darknet.assembler.visitor.ASTFieldVisitor;

public class BlwFieldVisitor extends BlwMemberVisitor implements ASTFieldVisitor {

    private final FieldBuilder.Nested<ClassBuilder> builder;

    public BlwFieldVisitor(FieldBuilder.Nested<ClassBuilder> builder) {
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
