package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.DalvikModifiers;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDalvikInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DalvikMethodVisitor extends DalvikMemberVisitor<MethodMember> implements ASTMethodVisitor {

    private final List<String> parameterNames = new ArrayList<>();
    private CodeBuilder codeBuilder;
    private DalvikCodeVisitor codeVisitor;

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
        if (codeVisitor == null) {
            codeBuilder = new CodeBuilder();
            codeVisitor = new DalvikCodeVisitor(codeBuilder, Map.of());
        }
        return codeVisitor;
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
        if (codeVisitor != null) {
            int incomingRegisters = member.getType().parameterTypes().size();
            if ((member.getAccess() & DalvikModifiers.ACC_STATIC) == 0) {
                incomingRegisters++;
            }

            Code code = codeBuilder
                    .arguments(incomingRegisters, codeVisitor.outRegisters())
                    .registers(Math.max(incomingRegisters, codeVisitor.registerCount()))
                    .build();
            codeVisitor.tryCatches().forEach(code::addTryCatch);
            member.setCode(code);
        }
    }

}
