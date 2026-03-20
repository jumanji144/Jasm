package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.visitor.ASTDalvikInstructionVisitor;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import me.darknet.dex.tree.definitions.instructions.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DalvikCodeVisitor implements ASTDalvikInstructionVisitor, Opcodes {

    private final CodeBuilder codeBuilder;
    private ASTInstruction currentInstructionAst;
    private Kind currentInstructionKind = Kind.NORMAL;
    private Map<String, Integer> registerMap = new HashMap<>();

    enum Kind {
        NORMAL,
        OBJECT,
        WIDE,
        RESULT
    }

    public DalvikCodeVisitor(CodeBuilder codeBuilder, Map<String, Integer> initialRegisterMap) {
        this.registerMap.putAll(initialRegisterMap);
        this.codeBuilder = codeBuilder;
    }

    public int getRegisterIndex(String registerName) {
        return registerMap.computeIfAbsent(registerName, k -> registerMap.size());
    }

    @Override
    public void visitInstruction(ASTInstruction instruction) {
        currentInstructionAst = instruction;
        // parse optional suffix
        currentInstructionKind = Kind.NORMAL;
        String content = instruction.content();
        int indexOfDash = content.lastIndexOf('-');
        if (indexOfDash == -1) {
            return;
        }

        String suffix = content.substring(indexOfDash + 1);
        switch (suffix) {
            case "object" -> currentInstructionKind = Kind.OBJECT;
            case "wide" -> currentInstructionKind = Kind.WIDE;
            case "result" -> currentInstructionKind = Kind.RESULT;
        }
    }

    @Override
    public void visitNop() {
        codeBuilder.add(new NopInstruction());
    }

    @Override
    public void visitMove(ASTIdentifier to, ASTIdentifier from) {
        int toIndex = getRegisterIndex(to.literal());
        int fromIndex = getRegisterIndex(from.literal());
        codeBuilder.add(switch (currentInstructionKind) {
            case OBJECT -> new MoveObjectInstruction(toIndex, fromIndex);
            case WIDE -> new MoveWideInstruction(toIndex, fromIndex);
            default -> new MoveInstruction(toIndex, fromIndex);
        });
    }

    @Override
    public void visitMoveResult(ASTIdentifier to) {
        int toIndex = getRegisterIndex(to.literal());
        codeBuilder.add(switch (currentInstructionKind) {
            case OBJECT -> new MoveResultInstruction(Result.OBJECT, toIndex);
            case WIDE -> new MoveResultInstruction(Result.WIDE, toIndex);
            default -> new MoveResultInstruction(Result.NORMAL, toIndex);
        });
    }

    @Override
    public void visitMoveException(ASTIdentifier to) {
        int toIndex = getRegisterIndex(to.literal());
        codeBuilder.add(new MoveExceptionInstruction(toIndex));
    }

    @Override
    public void visitReturn(ASTIdentifier returnValue) {
        int returnIndex = getRegisterIndex(returnValue.literal());
        codeBuilder.add(switch (currentInstructionKind) {
            case OBJECT -> new ReturnInstruction(returnIndex, Return.OBJECT);
            case WIDE -> new ReturnInstruction(returnIndex, Return.WIDE);
            default -> new ReturnInstruction(returnIndex);
        });
    }

    @Override
    public void visitReturnVoid() {
        codeBuilder.add(new ReturnInstruction());
    }

    @Override
    public void visitConst(ASTIdentifier to, ASTElement value) {
        int toIndex = getRegisterIndex(to.literal());
        switch (currentInstructionAst.identifier().content()) {
            case "const" -> {
                ASTNumber constValue = (ASTNumber) value;
                if (constValue.isFloatingPoint()) {
                    // convert float to int bits
                    int intBits = 0;
                    if (constValue.isWide()) {
                        intBits = Float.floatToIntBits((float) constValue.asDouble());
                    } else {
                        intBits = Float.floatToIntBits(constValue.asFloat());
                    }
                    codeBuilder.add(new ConstInstruction(toIndex, intBits));
                } else {
                    codeBuilder.add(new ConstInstruction(toIndex, constValue.asInt()));
                }
            }
            case "const-wide" -> {
                ASTNumber constValue = (ASTNumber) value;
                if (constValue.isFloatingPoint()) {
                    // convert double to long bits
                    long longBits = 0;
                    if (constValue.isWide()) {
                        longBits = Double.doubleToLongBits(constValue.asDouble());
                    } else {
                        longBits = Double.doubleToLongBits((double) constValue.asFloat());
                    }
                    codeBuilder.add(new ConstWideInstruction(toIndex, longBits));
                } else {
                    codeBuilder.add(new ConstWideInstruction(toIndex, constValue.asLong()));
                }
            }
            case "const-string" -> {
                ASTString constValue = (ASTString) value;
                codeBuilder.add(new ConstStringInstruction(toIndex, constValue.content());
            }
            case "const-class" -> {
                ASTIdentifier constValue = (ASTIdentifier) value;
                codeBuilder.add(new ConstClassInstruction(toIndex, constValue.literal()));
            }
        }
    }

    @Override
    public void visitMonitorEnter(ASTIdentifier register) {

    }

    @Override
    public void visitMonitorExit(ASTIdentifier register) {

    }

    @Override
    public void visitCheckCast(ASTIdentifier register, ASTIdentifier type) {

    }

    @Override
    public void visitInstanceOf(ASTIdentifier result, ASTIdentifier check, ASTIdentifier type) {

    }

    @Override
    public void visitArrayLength(ASTIdentifier result, ASTIdentifier array) {

    }

    @Override
    public void visitNewInstance(ASTIdentifier result, ASTIdentifier type) {

    }

    @Override
    public void visitNewArray(ASTIdentifier result, ASTIdentifier size, ASTIdentifier type) {

    }

    @Override
    public void visitFilledNewArray(ASTArray args, ASTIdentifier type) {

    }

    @Override
    public void visitFillArrayData(ASTIdentifier to, ASTArray array) {

    }

    @Override
    public void visitFillArrayDataPayload(ASTNumber elementWidth, ASTArray elements) {

    }

    @Override
    public void visitThrow(ASTIdentifier exception) {

    }

    @Override
    public void visitGoto(ASTIdentifier label) {

    }

    @Override
    public void visitPackedSwitch(ASTObject packedSwitchObject) {

    }

    @Override
    public void visitSparseSwitch(ASTObject sparseSwitchObject) {

    }

    @Override
    public void visitCmp(ASTIdentifier to, ASTIdentifier from1, ASTIdentifier from2) {

    }

    @Override
    public void visitIf(ASTIdentifier a, ASTIdentifier b, ASTIdentifier label) {

    }

    @Override
    public void visitIfZero(ASTIdentifier a, ASTIdentifier label) {

    }

    @Override
    public void visitArrayOperation(ASTIdentifier array, ASTIdentifier index, ASTIdentifier value) {

    }

    @Override
    public void visitVirtualFieldOperation(ASTIdentifier value, ASTIdentifier instance, ASTIdentifier path, ASTIdentifier descriptor) {

    }

    @Override
    public void visitStaticFieldOperation(ASTIdentifier value, ASTIdentifier path, ASTIdentifier descriptor) {

    }

    @Override
    public void visitInvoke(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor) {

    }

    @Override
    public void visitInvokeCustom(ASTArray registers, ASTIdentifier name, ASTIdentifier type, ASTArray handle, ASTArray arguments) {

    }

    @Override
    public void visitInvokePolymorphic(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor, ASTIdentifier proto) {

    }

    @Override
    public void visitUnaryOperation(ASTIdentifier to, ASTIdentifier from) {

    }

    @Override
    public void visitLabel(@NotNull ASTIdentifier label) {

    }

    @Override
    public void visitLineNumber(ASTNumber line) {

    }

    @Override
    public void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type) {

    }

    @Override
    public void visitEnd() {

    }
}
