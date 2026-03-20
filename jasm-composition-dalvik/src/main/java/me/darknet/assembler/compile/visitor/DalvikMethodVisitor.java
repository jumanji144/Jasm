package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDalvikInstructionVisitor;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.dex.tree.definitions.MethodMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DalvikMethodVisitor extends DalvikMemberVisitor<MethodMember> implements ASTMethodVisitor {

    private List<String> parameterNames = new ArrayList<>();

    public DalvikMethodVisitor(MethodMember member) {
        super(member);
    }

    @Override
    public void visitParameter(int index, ASTIdentifier name) {
        while (parameterNames.size() <= index) {
            parameterNames.add(null);
        }
        parameterNames.set(index, name.literal());
    }

    @Override
    public void visitAnnotationDefaultValue(ASTElement defaultValue) {

    }

    @Override
    public ASTDalvikInstructionVisitor visitDalvikCode(@NotNull ErrorCollector collector) {
        return ASTMethodVisitor.super.visitDalvikCode(collector);
    }

    @Override
    public ASTAnnotationVisitor visitVisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
        return null;
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
        return null;
    }

    @Override
    public void visitEnd() {
        member.setParameterNames(parameterNames);
    }

}
