package me.darknet.assembler.instructions.dalvik;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.visitor.ASTDalvikInstructionVisitor;

public class DalvikInstructions extends Instructions<ASTDalvikInstructionVisitor> {

    @Override
    protected void registerInstructions() {
        register("nop", ops(), (inst, visitor) -> visitor.visitNop());
        register("move", ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMove(inst.argument(0), inst.argument(1)));
        register("move-wide", ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMove(inst.argument(0), inst.argument(1)));
        register("move-object", ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMove(inst.argument(0), inst.argument(1)));
        register("move-result", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMoveResult(inst.argument(0)));
        register("move-result-wide", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMoveResult(inst.argument(0)));
        register("move-result-object", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMoveResult(inst.argument(0)));
        register("move-exception", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMoveException(inst.argument(0)));
        register("return-void", ops(), (inst, visitor) -> visitor.visitReturnVoid());
        register("return", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitReturn(inst.argument(0)));
        register("return-wide", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitReturn(inst.argument(0)));
        register("return-object", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitReturn(inst.argument(0)));
        register("const", ops(DefaultOperands.LITERAL, DefaultOperands.NUMBER),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1)));
        register("const-string", ops(DefaultOperands.LITERAL, DefaultOperands.STRING),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1)));
        register("const-class", ops(DefaultOperands.LITERAL, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1)));
        register("const-method-handle", ops(DefaultOperands.LITERAL, DalvikOperands.HANDLE),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1)));
        register("const-method-type", ops(DefaultOperands.LITERAL, DalvikOperands.METHOD_TYPE),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1)));
        register("monitor-enter", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMonitorEnter(inst.argument(0)));
        register("monitor-exit", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitMonitorExit(inst.argument(0)));
        register("check-cast", ops(DefaultOperands.LITERAL, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitCheckCast(inst.argument(0), inst.argument(1)));
        register("instance-of", ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitInstanceOf(inst.argument(0),
                        inst.argument(1), inst.argument(2)));
        register("array-length", ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitArrayLength(inst.argument(0), inst.argument(1)));
        register("new-instance", ops(DefaultOperands.LITERAL, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitNewInstance(inst.argument(0), inst.argument(1)));
        register("new-array", ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitNewArray(inst.argument(0),
                        inst.argument(1), inst.argument(2)));
        register("filled-new-array", ops(DalvikOperands.REGISTER_ARRAY, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitFilledNewArray(inst.argumentArray(0), inst.argument(1)));
        register("fill-array-data", ops(DefaultOperands.LITERAL, DefaultOperands.IDENTIFIER),
                (inst, visitor) -> visitor.visitFillArrayData(inst.argument(0), inst.argument(1)));
        register("fill-array-data-payload", ops(DefaultOperands.INTEGER, DalvikOperands.DATA_ARRAY),
                (inst, visitor) -> visitor.visitFillArrayDataPayload(inst.argument(0, ASTNumber.class),
                        inst.argumentArray(1)));
        register("throw", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitThrow(inst.argument(0)));
        register("goto", ops(DefaultOperands.LABEL),
                (inst, visitor) -> visitor.visitGoto(inst.argument(0)));
        register("packed-switch", ops(DalvikOperands.PACKED_SWITCH),
                (inst, visitor) -> visitor.visitPackedSwitch(inst.argumentObject(0)));
        register("sparse-switch", ops(DalvikOperands.SPARSE_SWITCH),
                (inst, visitor) -> visitor.visitSparseSwitch(inst.argumentObject(0)));
        registerCmp("cmpl-float", "cmpg-float", "cmpl-double", "cmpg-double", "cmp-long");
        registerIf("if-eq", "if-ne", "if-lt", "if-ge", "if-gt", "if-le",
                    "if-eqz", "if-nez");
        registerArrayOperation("aget", "aget-object", "aget-boolean", "aget-byte",
                               "aget-char", "aget-short", "aput", "aput-object",
                               "aput-boolean", "aput-byte", "aput-char", "aput-short");
        registerVirtualFieldOperation("iget", "iget-object", "iget-boolean", "iget-byte",
                                      "iget-char", "iget-short", "iput", "iput-object",
                                      "iput-boolean", "iput-byte", "iput-char", "iput-short");
        registerStaticFieldOperation("sget", "sget-object", "sget-boolean", "sget-byte",
                                        "sget-char", "sget-short", "sput", "sput-object",
                                        "sput-boolean", "sput-byte", "sput-char", "sput-short");


    }

    void registerCmp(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.LABEL),
                    (inst, visitor) -> visitor.visitCmp(inst.argument(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerIf(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LABEL),
                    (inst, visitor) -> visitor.visitIf(inst.argument(0), inst.argument(1)));
        }
    }

    void registerArrayOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitArrayOperation(inst.argument(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerVirtualFieldOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitVirtualFieldOperation(inst.argument(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerStaticFieldOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitStaticFieldOperation(inst.argument(0), inst.argument(1)));
        }
    }

}
