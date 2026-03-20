package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.compile.ConstantMapper;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.dex.tree.definitions.FieldMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DalvikFieldVisitor extends DalvikMemberVisitor<FieldMember> implements ASTFieldVisitor {
    public DalvikFieldVisitor(FieldMember member) {
        super(member);
    }

    @Override
    public void visitValue(ASTElement value) {
        member.setStaticValue(value == null ? null : ConstantMapper.fromConstant(value));
    }
}
