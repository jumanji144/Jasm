package me.darknet.assembler.instructions.jvm;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;

public class JvmInstructions extends Instructions<ASTJvmInstructionVisitor> {

    public final static JvmInstructions INSTANCE = new JvmInstructions();

    private JvmInstructions() {
        super();
    }

    @Override
    protected void registerInstructions() {
        registerSimple(
                "nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4",
                "iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1",
                "iaload", "laload", "faload", "daload", "aaload", "baload", "caload", "saload", "iastore", "lastore",
                "fastore", "dastore", "aastore", "bastore", "castore", "sastore", "pop", "pop2", "dup", "dup_x1",
                "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub",
                "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem",
                "ineg", "lneg", "fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land", "ior",
                "lor", "ixor", "lxor", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l",
                "d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ireturn", "lreturn", "freturn",
                "dreturn", "areturn", "return", "arraylength", "athrow", "monitorenter", "monitorexit"
        );

        registerIntProcessors("bipush", "sipush");

        registerTypeProcessors("anewarray", "checkcast", "instanceof");

        register(
                "new", ops(DefaultOperands.LITERAL),
                (inst, visitor) -> visitor.visitTypeInsn(inst.argument(0, ASTIdentifier.class))
        );

        register(
                "newarray", ops(JvmOperands.NEW_ARRAY_TYPE),
                (inst, visitor) -> visitor.visitNewArrayInsn(inst.argument(0, ASTIdentifier.class))
        );

        register(
                "ldc", ops(JvmOperands.CONSTANT),
                (inst, visitor) -> visitor.visitLdcInsn(inst.argument(0, ASTElement.class))
        );

        registerVarProcessors(
                "iload", "lload", "fload", "dload", "aload", "istore", "lstore", "fstore", "dstore", "astore", "ret"
        );

        register(
                "iinc", ops(DefaultOperands.LITERAL, DefaultOperands.INTEGER),
                (inst, visitor) -> visitor
                        .visitIincInsn(inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTNumber.class))
        );

        registerJumpProcessors(
                "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge",
                "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ifnull", "ifnonnull"
        );
        register(
                "tableswitch", ops(JvmOperands.TABLE_SWITCH),
                (inst, visitor) -> visitor.visitTableSwitchInsn(inst.argument(0, ASTObject.class))
        );
        register(
                "lookupswitch", ops(JvmOperands.LOOKUP_SWITCH),
                (inst, visitor) -> visitor.visitLookupSwitchInsn(inst.argument(0, ASTObject.class))
        );

        registerFieldProcessors("getstatic", "putstatic", "getfield", "putfield");

        registerMethodProcessors(
                "invokevirtual", "invokespecial", "invokestatic", "invokeinterface", "invokevirtualinterface",
                "invokestaticinterface", "invokespecialinterface"
        );

        register(
                "invokedynamic",
                ops(DefaultOperands.LITERAL, DefaultOperands.DESCRIPTOR, JvmOperands.HANDLE, JvmOperands.ARGS),
                (inst, visitor) -> visitor.visitInvokeDynamicInsn(
                        inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTIdentifier.class),
                        inst.argument(2, ASTArray.class), inst.argument(3, ASTArray.class)
                )
        );

        register(
                "multianewarray", ops(DefaultOperands.LITERAL, DefaultOperands.INTEGER),
                (inst, visitor) -> visitor.visitMultiANewArrayInsn(
                        inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTNumber.class)
                )
        );

        register(
                "jsr_w", ops(DefaultOperands.LABEL),
                (inst, visitor) -> visitor.visitJumpInsn(inst.argument(0, ASTIdentifier.class))
        );
        register(
                "goto_w", ops(DefaultOperands.LABEL),
                (inst, visitor) -> visitor.visitJumpInsn(inst.argument(0, ASTIdentifier.class))
        );
        register(
                "ldc_w", ops(JvmOperands.CONSTANT),
                (inst, visitor) -> visitor.visitLdcInsn(inst.argument(0, ASTElement.class))
        );
        register(
                "ldc2_w", ops(JvmOperands.CONSTANT),
                (inst, visitor) -> visitor.visitLdcInsn(inst.argument(0, ASTNumber.class))
        );

        // intrinsics
        register(
                "line", ops(DefaultOperands.IDENTIFIER, DefaultOperands.INTEGER),
                (inst, visitor) -> visitor
                        .visitLineNumber(inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTNumber.class))
        );
    }

    void registerSimple(String... names) {
        for (String name : names) {
            register(name, ops(), (inst, visitor) -> visitor.visitInsn());
        }
    }

    void registerIntProcessors(String... names) {
        for (String name : names) {
            register(
                    name, ops(DefaultOperands.INTEGER),
                    (inst, visitor) -> visitor.visitIntInsn(inst.argument(0, ASTNumber.class))
            );
        }
    }

    void registerVarProcessors(String... names) {
        for (String name : names) {
            register(
                    name, ops(DefaultOperands.LITERAL),
                    (inst, visitor) -> visitor.visitVarInsn(inst.argument(0, ASTIdentifier.class))
            );
        }
    }

    void registerJumpProcessors(String... names) {
        for (String name : names) {
            register(
                    name, ops(DefaultOperands.LABEL),
                    (inst, visitor) -> visitor.visitJumpInsn(inst.argument(0, ASTIdentifier.class))
            );
        }
    }

    void registerFieldProcessors(String... names) {
        for (String name : names) {
            register(
                    name, ops(DefaultOperands.LITERAL, DefaultOperands.FIELD_DESCRIPTOR),
                    (inst, visitor) -> visitor.visitFieldInsn(
                            inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTIdentifier.class)
                    )
            );
        }
    }

    void registerMethodProcessors(String... names) {
        for (String name : names) {
            register(
                    name, ops(DefaultOperands.LITERAL, DefaultOperands.METHOD_DESCRIPTOR),
                    (inst, visitor) -> visitor.visitMethodInsn(
                            inst.argument(0, ASTIdentifier.class), inst.argument(1, ASTIdentifier.class)
                    )
            );
        }
    }

    void registerTypeProcessors(String... names) {
        for (String name : names) {
            register(name, ops(JvmOperands.TYPE), (inst, visitor) -> {
                visitor.visitTypeInsn(inst.argument(0, ASTIdentifier.class));
            });
        }
    }
}
