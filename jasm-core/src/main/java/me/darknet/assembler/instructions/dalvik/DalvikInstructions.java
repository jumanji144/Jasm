package me.darknet.assembler.instructions.dalvik;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.visitor.ASTDalvikInstructionVisitor;

/**
 * Registers all Dalvik instructions with their respective operand types and visitors.
 */
public class DalvikInstructions extends Instructions<ASTDalvikInstructionVisitor> {
    public static final DalvikInstructions INSTANCE = new DalvikInstructions();

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
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1, ASTElement.class)));
        register("const-wide", ops(DefaultOperands.LITERAL, DefaultOperands.NUMBER),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1, ASTElement.class)));
        register("const-string", ops(DefaultOperands.LITERAL, DefaultOperands.STRING),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1, ASTElement.class)));
        register("const-class", ops(DefaultOperands.LITERAL, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1, ASTElement.class)));
        register("const-method-handle", ops(DefaultOperands.LITERAL, DalvikOperands.HANDLE),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1, ASTElement.class)));
        register("const-method-type", ops(DefaultOperands.LITERAL, DalvikOperands.METHOD_TYPE),
                (inst, visitor) -> visitor.visitConst(inst.argument(0), inst.argument(1, ASTElement.class)));
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
        register("filled-new-array/range", ops(DalvikOperands.REGISTER_ARRAY, DalvikOperands.CLASS_TYPE),
                (inst, visitor) -> visitor.visitFilledNewArray(inst.argumentArray(0), inst.argument(1)));
        register("fill-array-data", ops(DefaultOperands.LITERAL, DalvikOperands.DATA_ARRAY),
                (inst, visitor) -> visitor.visitFillArrayData(inst.argument(0), inst.argument(1, ASTElement.class)));
        register("throw", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitThrow(inst.argument(0)));
        register("goto", ops(DefaultOperands.LABEL),
                (inst, visitor) -> visitor.visitGoto(inst.argument(0)));
        register("packed-switch", ops(DefaultOperands.LITERAL, DalvikOperands.PACKED_SWITCH),
                (inst, visitor) -> visitor.visitPackedSwitch(inst.argument(0), inst.argumentObject(1)));
        register("sparse-switch", ops(DefaultOperands.LITERAL, DalvikOperands.SPARSE_SWITCH),
                (inst, visitor) -> visitor.visitSparseSwitch(inst.argument(0), inst.argumentObject(1)));
        registerCmp("cmpl-float", "cmpg-float", "cmpl-double", "cmpg-double", "cmp-long");
        registerBinaryOperation(
                "add-int", "sub-int", "mul-int", "div-int", "rem-int", "and-int", "or-int", "xor-int",
                "shl-int", "shr-int", "ushr-int", "add-long", "sub-long", "mul-long", "div-long", "rem-long",
                "and-long", "or-long", "xor-long", "shl-long", "shr-long", "ushr-long", "add-float",
                "sub-float", "mul-float", "div-float", "rem-float", "add-double", "sub-double", "mul-double",
                "div-double", "rem-double"
        );
        registerBinary2AddrOperation(
                "add-int/2addr", "sub-int/2addr", "mul-int/2addr", "div-int/2addr", "rem-int/2addr",
                "and-int/2addr", "or-int/2addr", "xor-int/2addr", "shl-int/2addr", "shr-int/2addr",
                "ushr-int/2addr", "add-long/2addr", "sub-long/2addr", "mul-long/2addr", "div-long/2addr",
                "rem-long/2addr", "and-long/2addr", "or-long/2addr", "xor-long/2addr", "shl-long/2addr",
                "shr-long/2addr", "ushr-long/2addr", "add-float/2addr", "sub-float/2addr", "mul-float/2addr",
                "div-float/2addr", "rem-float/2addr", "add-double/2addr", "sub-double/2addr", "mul-double/2addr",
                "div-double/2addr", "rem-double/2addr"
        );
        registerBinaryLiteralOperation(
                "add-int/lit16", "rsub-int", "mul-int/lit16", "div-int/lit16", "rem-int/lit16",
                "and-int/lit16", "or-int/lit16", "xor-int/lit16", "add-int/lit8", "rsub-int/lit8",
                "mul-int/lit8", "div-int/lit8", "rem-int/lit8", "and-int/lit8", "or-int/lit8", "xor-int/lit8",
                "shl-int/lit8", "shr-int/lit8", "ushr-int/lit8"
        );
        registerIf("if-eq", "if-ne", "if-lt", "if-ge", "if-gt", "if-le");
        registerIfZero("if-eqz", "if-nez", "if-ltz", "if-gez", "if-gtz", "if-lez");
        registerArrayOperation("aget", "aget-object", "aget-wide", "aget-boolean", "aget-byte",
                               "aget-char", "aget-short", "aput", "aput-object", "aput-wide",
                               "aput-boolean", "aput-byte", "aput-char", "aput-short");
        registerVirtualFieldOperation("iget", "iget-object", "iget-wide", "iget-boolean", "iget-byte",
                                      "iget-char", "iget-short", "iput", "iput-object", "iput-wide",
                                      "iput-boolean", "iput-byte", "iput-char", "iput-short");
        registerStaticFieldOperation("sget", "sget-object", "sget-wide", "sget-boolean", "sget-byte",
                                        "sget-char", "sget-short", "sput", "sput-object", "sput-wide",
                                        "sput-boolean", "sput-byte", "sput-char", "sput-short");
        registerInvoke("invoke-virtual", "invoke-super", "invoke-direct", "invoke-static",
                       "invoke-interface", "invoke-virtual/range", "invoke-super/range",
                       "invoke-direct/range", "invoke-static/range", "invoke-interface/range");
        registerInvokeCustom("invoke-custom", "invoke-custom/range");
        registerInvokePolymorphic("invoke-polymorphic", "invoke-polymorphic/range");
        registerUnaryOperation(
                "neg-int", "not-int", "neg-long", "not-long", "neg-float", "neg-double",
                "int-to-long", "int-to-float", "int-to-double", "long-to-int", "long-to-float",
                "long-to-double", "float-to-int", "float-to-long", "float-to-double",
                "double-to-int", "double-to-long", "double-to-float",
                "int-to-byte", "int-to-char", "int-to-short"
        );

        register("line", ops(DefaultOperands.INTEGER), (inst, visitor) -> visitor.visitLineNumber(inst.argument(0, ASTNumber.class)));


    }

    void registerBinaryOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitBinaryOperation(inst.argument(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerBinary2AddrOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitBinary2AddrOperation(inst.argument(0), inst.argument(1)));
        }
    }

    void registerBinaryLiteralOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.NUMBER),
                    (inst, visitor) -> visitor.visitBinaryLiteralOperation(
                            inst.argument(0), inst.argument(1), inst.argument(2, ASTNumber.class)));
        }
    }

    void registerCmp(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitCmp(inst.argument(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerIf(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.LABEL),
                    (inst, visitor) -> visitor.visitIf(inst.argument(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerIfZero(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LABEL),
                    (inst, visitor) -> visitor.visitIfZero(inst.argument(0), inst.argument(1)));
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
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.FIELD_DESCRIPTOR),
                    (inst, visitor) -> visitor.visitVirtualFieldOperation(inst.argument(0), inst.argument(1), inst.argument(2), inst.argument(3)));
        }
    }

    void registerStaticFieldOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, DefaultOperands.FIELD_DESCRIPTOR),
                    (inst, visitor) -> visitor.visitStaticFieldOperation(inst.argument(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerInvoke(String... names) {
        for (String name : names) {
            register(name, ops(DalvikOperands.REGISTER_ARRAY, DefaultOperands.LITERAL, DefaultOperands.METHOD_DESCRIPTOR),
                    (inst, visitor) -> visitor.visitInvoke(inst.argumentArray(0), inst.argument(1), inst.argument(2)));
        }
    }

    void registerInvokeCustom(String... names) {
        for (String name : names) {
            register(name,
                    ops(DalvikOperands.REGISTER_ARRAY, DefaultOperands.LITERAL, DefaultOperands.DESCRIPTOR, DalvikOperands.HANDLE, DalvikOperands.ARGS_ARRAY),
                    (inst, visitor) -> visitor.visitInvokeCustom(inst.argumentArray(0), inst.argument(1), inst.argument(2), inst.argument(3, ASTElement.class), inst.argumentArray(4)));

        }
    }

    void registerInvokePolymorphic(String... names) {
        for (String name : names) {
            register(name,
                    ops(DalvikOperands.REGISTER_ARRAY, DefaultOperands.LITERAL, DefaultOperands.METHOD_DESCRIPTOR, DefaultOperands.METHOD_DESCRIPTOR),
                    (inst, visitor) -> visitor.visitInvokePolymorphic(inst.argumentArray(0), inst.argument(1), inst.argument(2), inst.argument(3)));

        }
    }

    void registerUnaryOperation(String... names) {
        for (String name : names) {
            register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitUnaryOperation(inst.argument(0), inst.argument(1)));
        }
    }
}
