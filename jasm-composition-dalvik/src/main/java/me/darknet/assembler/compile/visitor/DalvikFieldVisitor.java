package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.DalvikConstantMapper;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.dex.tree.definitions.FieldMember;

public class DalvikFieldVisitor extends DalvikMemberVisitor<FieldMember> implements ASTFieldVisitor {
    public DalvikFieldVisitor(FieldMember member) {
        super(member);
    }

    @Override
    public void visitValue(ASTElement value) {
        member.setStaticValue(value == null ? null : DalvikConstantMapper.fromConstant(value));
    }
}
