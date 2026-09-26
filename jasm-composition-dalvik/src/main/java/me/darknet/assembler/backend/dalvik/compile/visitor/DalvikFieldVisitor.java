package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.compile.DalvikConstantMapper;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.dex.tree.definitions.FieldMember;
import org.jetbrains.annotations.Nullable;

/**
 * Visits a Dalvik field declaration and populates its dex field member.
 */
public class DalvikFieldVisitor extends DalvikMemberVisitor<FieldMember> implements ASTFieldVisitor {
    public DalvikFieldVisitor(FieldMember member) {
        super(member);
    }

    @Override
    public void visitValue(@Nullable ASTElement value) {
        member.setStaticValue(value == null ? null : DalvikConstantMapper.fromConstant(value));
    }
}
