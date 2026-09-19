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
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DalvikMethodVisitor extends DalvikMemberVisitor<MethodMember> implements ASTMethodVisitor {

    private final List<String> parameterNames = new ArrayList<>();
    private Integer declaredRegisterCount;
    private int incomingRegisterCount;
    private CodeBuilder codeBuilder;
    private DalvikCodeVisitor codeVisitor;

    public DalvikMethodVisitor(MethodMember member) {
        super(member);
    }

    @Override
    public void visitRegisterCount(int registers) {
        if (registers < 0) {
            throw new IllegalStateException("Method register count must be nonnegative");
        }
        declaredRegisterCount = registers;
    }

    @Override
    public void visitParameter(int index, @NotNull ASTIdentifier name) {
        while (parameterNames.size() <= index) {
            parameterNames.add(null);
        }
        parameterNames.set(index, name.literal());
    }

    @Override
    public void visitDeclaredException(@NotNull ASTIdentifier exceptionType) {
        member.addThrownType(exceptionType.literal());
    }

    @Override
    public void visitAnnotationDefaultValue(ASTElement defaultValue) {
        throw new IllegalStateException("Dalvik annotation default values are not supported by the current dex tree");
    }

    @Override
    public ASTDalvikInstructionVisitor visitDalvikCode(@NotNull ErrorCollector collector) {
        if (codeVisitor == null) {
            codeBuilder = new CodeBuilder();
            incomingRegisterCount = incomingRegisterWords();
            if (declaredRegisterCount == null && incomingRegisterCount > 0) {
                throw new IllegalStateException("Dalvik methods with parameters require registers");
            }
            int registerLimit = declaredRegisterCount == null ? -1 : declaredRegisterCount;
            int parameterBase = declaredRegisterCount == null
                    ? -1
                    : declaredRegisterCount - incomingRegisterCount;
            if (declaredRegisterCount != null && parameterBase < 0) {
                throw new IllegalStateException("Declared register count is smaller than incoming parameters");
            }
            codeVisitor = new DalvikCodeVisitor(
                    codeBuilder,
                    parameterRegisters(parameterBase),
                    registerLimit,
                    parameterBase
            );
        }
        return codeVisitor;
    }

    private int incomingRegisterWords() {
        int words = (member.getAccess() & DalvikModifiers.ACC_STATIC) == 0 ? 1 : 0;
        for (var parameter : member.getType().parameterTypes()) {
            words += registerWords(parameter.descriptor());
        }
        return words;
    }

    private Map<String, Integer> parameterRegisters(int parameterBase) {
        Map<String, Integer> registers = new java.util.LinkedHashMap<>();
        int aliasBase = Math.max(parameterBase, 0);
        int offset = 0;
        if ((member.getAccess() & DalvikModifiers.ACC_STATIC) == 0) {
            registers.put("this", aliasBase);
            offset++;
        }
        for (int index = 0; index < member.getType().parameterTypes().size(); index++) {
            int register = aliasBase + offset;
            String name = index < parameterNames.size() ? parameterNames.get(index) : null;
            if (name != null) {
                registers.put(name, register);
            }
            registers.put("p" + index, register);
            offset += registerWords(member.getType().parameterTypes().get(index).descriptor());
        }
        return registers;
    }

    private static int registerWords(String descriptor) {
        return descriptor.length() == 1 && (descriptor.charAt(0) == 'J' || descriptor.charAt(0) == 'D') ? 2 : 1;
    }

    @Override
    public ASTAnnotationVisitor visitVisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
        throw new IllegalStateException("Dalvik parameter annotations are not supported by the current dex tree");
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
        throw new IllegalStateException("Dalvik parameter annotations are not supported by the current dex tree");
    }

    @Override
    public void visitEnd() {
        member.setParameterNames(parameterNames);
        if (codeVisitor != null) {
            int totalRegisters = Math.max(incomingRegisterCount, codeVisitor.getRegisterCount());
            if (declaredRegisterCount != null && totalRegisters > declaredRegisterCount) {
                throw new IllegalStateException("Code uses more registers than declared");
            }

            Code code = codeBuilder
                    .arguments(incomingRegisterCount, codeVisitor.getOutRegisters())
                    .registers(declaredRegisterCount == null ? totalRegisters : declaredRegisterCount)
                    .build();
            codeVisitor.getTryCatches().forEach(code::addTryCatch);
            DebugInformation debugInfo = debugInformation(code);
            if (debugInfo != null) {
                code.setDebugInfo(debugInfo);
            }
            member.setCode(code);
        }
    }

    private DebugInformation debugInformation(Code code) {
        List<DebugInformation.LineNumber> lineNumbers = new ArrayList<>();
        for (Instruction instruction : code.getInstructions()) {
            if (instruction instanceof Label label && label.lineNumber() != Label.UNASSIGNED) {
                lineNumbers.add(new DebugInformation.LineNumber(label, label.lineNumber()));
            }
        }
        if (lineNumbers.isEmpty() && parameterNames.isEmpty()) {
            return null;
        }
        return new DebugInformation(lineNumbers, List.copyOf(parameterNames), List.of());
    }

}
